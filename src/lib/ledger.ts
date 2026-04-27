import { randomUUID } from "node:crypto";
import { writeFileSync } from "node:fs";
import { join } from "node:path";
import { db } from "./db";

export type LedgerAuthor = "claude" | "user";

export type LedgerEntry = {
  id: string;
  body: string;
  author: LedgerAuthor;
  createdAt: string;
};

type Row = {
  id: string;
  body: string;
  author: string;
  created_at: string;
};

// In dev (`npm run dev`) cwd is the project root. If you ever run `next start`
// from a different directory this needs to be resolved differently. Same
// assumption as `src/app/log/page.tsx`.
const LEDGER_FILE = join(process.cwd(), "LEDGER.md");

function rowToEntry(row: Row): LedgerEntry {
  const author: LedgerAuthor = row.author === "user" ? "user" : "claude";
  return { id: row.id, body: row.body, author, createdAt: row.created_at };
}

export function listLedgerEntries(): LedgerEntry[] {
  const rows = db()
    .prepare(
      "SELECT id, body, author, created_at FROM ledger_entries ORDER BY created_at ASC"
    )
    .all() as Row[];
  return rows.map(rowToEntry);
}

export function listRecentLedgerEntries(limit: number): LedgerEntry[] {
  const rows = db()
    .prepare(
      "SELECT id, body, author, created_at FROM ledger_entries ORDER BY created_at DESC LIMIT ?"
    )
    .all(limit) as Row[];
  return rows.map(rowToEntry).reverse();
}

export function getLastLedgerEntry(): LedgerEntry | null {
  const row = db()
    .prepare(
      "SELECT id, body, author, created_at FROM ledger_entries ORDER BY created_at DESC LIMIT 1"
    )
    .get() as Row | undefined;
  return row ? rowToEntry(row) : null;
}

export function createLedgerEntry(input: {
  body: string;
  author: LedgerAuthor;
}): LedgerEntry {
  const id = randomUUID();
  const createdAt = new Date().toISOString();
  db()
    .prepare(
      "INSERT INTO ledger_entries (id, body, author, created_at) VALUES (?, ?, ?, ?)"
    )
    .run(id, input.body, input.author, createdAt);
  return { id, body: input.body, author: input.author, createdAt };
}

export function updateLedgerEntry(id: string, body: string): void {
  db()
    .prepare("UPDATE ledger_entries SET body = ? WHERE id = ?")
    .run(body, id);
}

export function deleteLedgerEntry(id: string): void {
  db().prepare("DELETE FROM ledger_entries WHERE id = ?").run(id);
}

export function countFragmentsSinceLastLedger(): number {
  const last = getLastLedgerEntry();
  if (!last) {
    const row = db()
      .prepare("SELECT COUNT(*) AS n FROM fragments")
      .get() as { n: number };
    return row.n;
  }
  const row = db()
    .prepare("SELECT COUNT(*) AS n FROM fragments WHERE created_at > ?")
    .get(last.createdAt) as { n: number };
  return row.n;
}

export function writeLedgerMirror(): void {
  const entries = listLedgerEntries();
  const lines: string[] = [
    "# LEDGER.md",
    "",
    "A journal of the collection itself — what's been gathered, what shape it's taking. Mirrored from `~/.commonplace/db.sqlite`; edits made in the app appear here, edits made here will be overwritten.",
    "",
  ];
  for (const e of entries) {
    lines.push("---");
    lines.push("");
    lines.push(`### ${e.createdAt.slice(0, 10)} — ${e.author}`);
    lines.push("");
    lines.push(e.body.trim());
    lines.push("");
  }
  writeFileSync(LEDGER_FILE, lines.join("\n"), "utf8");
}
