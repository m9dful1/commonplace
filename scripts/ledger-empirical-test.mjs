// Empirical test for the Ledger pass option (Phase 4 design entry).
//
// Scenarios:
//   A.  First Ledger generation, no prior entry, against the user's eleven
//       fragments. Expect: a real entry — there's no prior to "pass" against.
//   B.  Second generation immediately after A, no new fragments. Expect: pass.
//   C.  Third generation with a contrived off-topic fragment ("watered the
//       basil") added on top of A's entry. Expect: pass (one thin off-topic
//       fragment is not enough to revise the entry).
//   D.  Fourth generation with two off-topic fragments. Expect: pass — these
//       fragments don't reshape the collection.
//
// Notes:
// - We do not write to the DB or to LEDGER.md from this script. It uses the
//   real prompt (same code path as the route) but a synthetic last-Ledger.

import Anthropic from "@anthropic-ai/sdk";
import Database from "better-sqlite3";
import { homedir } from "node:os";
import { join } from "node:path";
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";

// Load .env.local manually (no dotenv dep)
try {
  const envText = readFileSync(join(process.cwd(), ".env.local"), "utf8");
  for (const line of envText.split(/\r?\n/)) {
    const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
    if (m && !process.env[m[1]]) process.env[m[1]] = m[2];
  }
} catch {
  // .env.local may not exist; fall through to ANTHROPIC_API_KEY check
}

// Mirror of the prompt in src/lib/ledgerPrompt.ts. Keep these in sync — if you
// change the canonical prompt, paste the new copy here too. (The script can't
// import the .ts module without a TS loader, so we duplicate.)

const LEDGER_SYSTEM_PROMPT = `You are looking at a section of someone's commonplace book — fragments they've been collecting, with margin notes a previous Claude has left on some of them, and possibly one or two recent Ledger entries already on file.

Your job is to decide, first, whether a new Ledger entry is warranted at all. Most of the time, the answer is no.

**Step 1 — the pass gate. Apply this BEFORE drafting anything.**

The gate has two cases.

CASE A — there is no prior Ledger entry. Write a first entry as long as the collection has at least a handful of fragments worth characterizing (a few themes, a recurring source, or a recognizable shape, possibly with one or two outliers). Only return "pass" if the collection is so thin or scattered that there is genuinely nothing yet to mark — for example, fewer than three fragments, or fragments with no shared thread of any kind.

CASE B — there is a prior Ledger entry. Write a new entry only if the shape of the collection has genuinely changed since the last entry. The shape has changed if and only if at least one of these is true:
- a meaningfully new theme has emerged that the prior entry does not already name;
- a new source or thinker has entered the collection;
- a new tension, contradiction, or surprise has appeared that the prior entry does not mark.

The shape has NOT changed (and you should return "pass") if any of the following hold:
- the new fragments deepen, extend, or restate themes the prior entry already named — even if you could write a slightly different angle on them;
- only one or two thin or off-topic fragments have been added (a household note, a passing thought, a single off-cluster observation) without reshaping the section;
- the only thing you can offer is a meta-observation about how the cluster is "thickening", "winding down", or has "exhausted its momentum" — these are restatements, not new shape;
- you would be saying essentially the same thing the prior entry already says, in different words.

In Case B, pass is the correct answer most of the time. Do NOT write a paragraph and then append "pass". Do NOT hedge by writing an entry "in case it's useful". Either return a Ledger entry, or return only the word "pass". Never both.

**Step 2 — only if the gate clears, write the entry.**

Write a brief Ledger entry: a short paragraph noting what shape this section of the collection is taking. The Ledger is a journal of the collection itself, not of the person. Describe what they've been gathering — themes, recurring sources, specific concerns, tensions, surprises. Be specific: name the actual people they're quoting, the actual recurring concerns, not vague abstractions. Be brief: 2 to 4 sentences, never longer than a small paragraph.

If a fragment doesn't fit the cluster, name it without forcing it to. Most sections of a commonplace book have one rough shape and one or two outliers. Honor both.

Do not psychologize. Do not predict where they are going. Do not claim to know who they are. Do not quote their fragments back at length. The Ledger's only job is to mark what's accreting, the way someone might note what's been added to a shelf.`;

const MODEL = "claude-sonnet-4-6";

function buildUserMessage({ fragments, recentLedgerEntries }) {
  const parts = [];
  if (recentLedgerEntries.length > 0) {
    parts.push("Recent Ledger entries (oldest first):");
    parts.push("");
    for (const e of recentLedgerEntries) {
      parts.push(`(${e.createdAt.slice(0, 10)}) ${e.body.trim()}`);
      parts.push("");
    }
    parts.push("---");
    parts.push("");
    parts.push(
      "Recent fragments in the collection (oldest first; some predate the last Ledger entry, included for context):"
    );
  } else {
    parts.push("Recent fragments in the collection (oldest first):");
  }
  parts.push("");
  for (const f of fragments) {
    const date = f.createdAt.slice(0, 10);
    const head = f.source ? `${f.body.trim()} — ${f.source}` : f.body.trim();
    parts.push(`(${date}) ${head}`);
    for (const m of f.marginalia) parts.push(`    margin: ${m.trim()}`);
    parts.push("");
  }
  return parts.join("\n");
}

function loadDb() {
  const dbPath = join(homedir(), ".commonplace", "db.sqlite");
  const db = new Database(dbPath, { readonly: true });
  const fragRows = db
    .prepare(
      "SELECT id, body, source, created_at FROM fragments ORDER BY created_at ASC"
    )
    .all();
  const margRows = db
    .prepare(
      "SELECT fragment_id, body, created_at FROM marginalia ORDER BY created_at ASC"
    )
    .all();
  db.close();
  const margMap = new Map();
  for (const m of margRows) {
    if (!margMap.has(m.fragment_id)) margMap.set(m.fragment_id, []);
    margMap.get(m.fragment_id).push(m.body);
  }
  return fragRows.map((f) => ({
    body: f.body,
    source: f.source,
    createdAt: f.created_at,
    marginalia: margMap.get(f.id) ?? [],
  }));
}

async function call(client, fragments, recentLedgerEntries) {
  const userMessage = buildUserMessage({ fragments, recentLedgerEntries });
  const response = await client.messages.create({
    model: MODEL,
    max_tokens: 400,
    system: LEDGER_SYSTEM_PROMPT,
    messages: [{ role: "user", content: userMessage }],
  });
  const text = response.content
    .map((b) => (b.type === "text" ? b.text : ""))
    .join("")
    .trim();
  return { text, userMessage };
}

function isPass(text) {
  return /^pass[.!\s]*$/i.test(text.trim());
}

const apiKey = process.env.ANTHROPIC_API_KEY;
if (!apiKey) {
  console.error("ANTHROPIC_API_KEY is not set; cannot run empirical test.");
  process.exit(1);
}

const client = new Anthropic({ apiKey });

const fragments = loadDb();
console.log(`loaded ${fragments.length} fragments from local db`);

const offTopicOne = [
  {
    body: "Watered the basil. The leaves were starting to droop on the south side of the pot.",
    source: null,
    createdAt: "2026-04-26T23:50:00.000Z",
    marginalia: [],
  },
];

const offTopicTwo = [
  ...offTopicOne,
  {
    body: "Need to check tire pressure on the car this weekend before the trip.",
    source: null,
    createdAt: "2026-04-26T23:51:00.000Z",
    marginalia: [],
  },
];

const out = { runs: [] };

async function runScenario(label, fragSet, ledgerEntries) {
  console.log(`\n=== ${label} ===`);
  const { text } = await call(client, fragSet, ledgerEntries);
  const pass = isPass(text);
  console.log(`pass=${pass}`);
  console.log(text);
  out.runs.push({ label, pass, text, fragmentCount: fragSet.length });
  return text;
}

const a = await runScenario(
  "A — first ledger generation, no prior entry",
  fragments,
  []
);

let ledgerA;
if (isPass(a)) {
  console.log("\n[scenario A returned pass — using a synthetic prior entry]");
  ledgerA = {
    body: "You've been gathering Karpathy on culture, repertoire, and self-play in LLMs — savant-kid framing, the absence of inter-model continuity. Threaded with your own questions about whether the log idea was Claude's or yours, and an audit-method observation that turns out to be an external-memory pattern. One outlier: Brooke's fish tank.",
    createdAt: new Date().toISOString(),
  };
} else {
  ledgerA = { body: a, createdAt: new Date().toISOString() };
}

await runScenario(
  "B — same eleven fragments, with the entry from A as prior",
  fragments,
  [ledgerA]
);

await runScenario(
  "C — eleven fragments + one off-topic (basil)",
  [...fragments, ...offTopicOne],
  [ledgerA]
);

await runScenario(
  "D — eleven fragments + two off-topic (basil + tire pressure)",
  [...fragments, ...offTopicTwo],
  [ledgerA]
);

// Output filename: pass --out=<name> to override (e.g. phase4_5-pass-test.json)
const outArg = process.argv.find((a) => a.startsWith("--out="));
const outName = outArg ? outArg.slice("--out=".length) : "phase4-pass-test.json";
const outDir = join(process.cwd(), "screenshots");
mkdirSync(outDir, { recursive: true });
const outPath = join(outDir, outName);
writeFileSync(outPath, JSON.stringify(out, null, 2), "utf8");

const passSummary = out.runs
  .map((r) => `${r.label.split(" ")[0]}=${r.pass ? "pass" : "entry"}`)
  .join(" ");
console.log(`\n${passSummary}`);
console.log(`results written to ${outPath}`);
