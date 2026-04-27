import { anthropicClient } from "./anthropic";
import { db } from "./db";
import { listRecentLedgerEntries } from "./ledger";
import { createLetter, writeLettersMirror, type Letter } from "./letters";
import {
  LETTERS_SYSTEM_PROMPT,
  buildLetterUserMessage,
  type FragmentForLetter,
} from "./lettersPrompt";
import { LETTERS_MODEL } from "./models";

const MAX_FRAGMENTS = 30;
const RECENT_LEDGER_COUNT = 2;
const LETTERS_MAX_TOKENS = 800;

type FragRow = {
  id: string;
  body: string;
  source: string | null;
  created_at: string;
};

type MargRow = { fragment_id: string; body: string; created_at: string };

export function gatherLetterContext(): {
  fragments: FragmentForLetter[];
  fragmentIds: string[];
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
    fragmentIds: fragments.map((f) => f.id),
    recentLedgerEntries: recent,
  };
}

export async function generateLetter(): Promise<Letter> {
  const client = anthropicClient();
  const { fragments, fragmentIds, recentLedgerEntries } = gatherLetterContext();
  const today = new Date().toISOString().slice(0, 10);
  const userMessage = buildLetterUserMessage({
    fragments,
    recentLedgerEntries,
    today,
  });

  const response = await client.messages.create({
    model: LETTERS_MODEL,
    max_tokens: LETTERS_MAX_TOKENS,
    system: LETTERS_SYSTEM_PROMPT,
    messages: [{ role: "user", content: userMessage }],
  });

  const text = response.content
    .map((b) => (b.type === "text" ? b.text : ""))
    .join("")
    .trim();

  if (!text) {
    throw new Error("Letter generation returned empty body.");
  }

  const letter = createLetter({ body: text, fragmentsReferenced: fragmentIds });
  writeLettersMirror();
  return letter;
}
