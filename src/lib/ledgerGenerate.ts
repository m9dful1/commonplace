import { anthropicClient } from "./anthropic";
import { db } from "./db";
import {
  createLedgerEntry,
  listRecentLedgerEntries,
  writeLedgerMirror,
  type LedgerEntry,
} from "./ledger";
import {
  LEDGER_SYSTEM_PROMPT,
  buildLedgerUserMessage,
  isPassResponse,
  type FragmentForLedger,
} from "./ledgerPrompt";
import { MARGINALIA_MODEL } from "./models";

const MAX_FRAGMENTS = 15;
const RECENT_LEDGER_COUNT = 2;
const LEDGER_MAX_TOKENS = 400;

export type GenerateResult =
  | { status: "created"; entry: LedgerEntry; raw: string }
  | { status: "pass"; raw: string };

type FragRow = {
  id: string;
  body: string;
  source: string | null;
  created_at: string;
};

type MargRow = { fragment_id: string; body: string; created_at: string };

export function gatherLedgerContext(): {
  fragments: FragmentForLedger[];
  recentLedgerEntries: { body: string; createdAt: string }[];
} {
  const recent = listRecentLedgerEntries(RECENT_LEDGER_COUNT).map((e) => ({
    body: e.body,
    createdAt: e.createdAt,
  }));

  const fragRows = db()
    .prepare(
      "SELECT id, body, source, created_at FROM fragments ORDER BY created_at DESC LIMIT ?"
    )
    .all(MAX_FRAGMENTS) as FragRow[];
  const fragments = fragRows.reverse();

  const margRows = db()
    .prepare(
      "SELECT fragment_id, body, created_at FROM marginalia ORDER BY created_at ASC"
    )
    .all() as MargRow[];
  const margMap = new Map<string, string[]>();
  for (const m of margRows) {
    const list = margMap.get(m.fragment_id) ?? [];
    list.push(m.body);
    margMap.set(m.fragment_id, list);
  }

  return {
    fragments: fragments.map((f) => ({
      body: f.body,
      source: f.source,
      createdAt: f.created_at,
      marginalia: margMap.get(f.id) ?? [],
    })),
    recentLedgerEntries: recent,
  };
}

export async function generateLedgerEntry(): Promise<GenerateResult> {
  const client = anthropicClient();
  const { fragments, recentLedgerEntries } = gatherLedgerContext();
  const userMessage = buildLedgerUserMessage({ fragments, recentLedgerEntries });

  const response = await client.messages.create({
    model: MARGINALIA_MODEL,
    max_tokens: LEDGER_MAX_TOKENS,
    system: LEDGER_SYSTEM_PROMPT,
    messages: [{ role: "user", content: userMessage }],
  });

  const text = response.content
    .map((block) => (block.type === "text" ? block.text : ""))
    .join("")
    .trim();

  if (isPassResponse(text)) {
    return { status: "pass", raw: text };
  }

  const entry = createLedgerEntry({ body: text, author: "claude" });
  writeLedgerMirror();
  return { status: "created", entry, raw: text };
}
