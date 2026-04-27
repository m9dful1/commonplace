import { randomUUID } from "node:crypto";
import { writeFileSync } from "node:fs";
import { join } from "node:path";
import { db } from "./db";

export type Letter = {
  id: string;
  body: string;
  fragmentsReferenced: string[];
  createdAt: string;
};

type Row = {
  id: string;
  body: string;
  fragments_referenced: string | null;
  created_at: string;
};

// In dev (`npm run dev`) cwd is the project root. If you ever run `next start`
// from a different directory this needs to be resolved differently. Same
// assumption as `src/app/log/page.tsx`.
const LETTERS_FILE = join(process.cwd(), "LETTERS.md");

function rowToLetter(row: Row): Letter {
  let refs: string[] = [];
  if (row.fragments_referenced) {
    try {
      const parsed = JSON.parse(row.fragments_referenced);
      if (Array.isArray(parsed)) refs = parsed.filter((s) => typeof s === "string");
    } catch {
      // column corrupt — empty
    }
  }
  return {
    id: row.id,
    body: row.body,
    fragmentsReferenced: refs,
    createdAt: row.created_at,
  };
}

export function listLetters(): Letter[] {
  const rows = db()
    .prepare(
      "SELECT id, body, fragments_referenced, created_at FROM letters ORDER BY created_at DESC"
    )
    .all() as Row[];
  return rows.map(rowToLetter);
}

export function createLetter(input: {
  body: string;
  fragmentsReferenced?: string[];
}): Letter {
  const id = randomUUID();
  const createdAt = new Date().toISOString();
  const refs = input.fragmentsReferenced && input.fragmentsReferenced.length
    ? JSON.stringify(input.fragmentsReferenced)
    : null;
  db()
    .prepare(
      "INSERT INTO letters (id, body, fragments_referenced, created_at) VALUES (?, ?, ?, ?)"
    )
    .run(id, input.body, refs, createdAt);
  return {
    id,
    body: input.body,
    fragmentsReferenced: input.fragmentsReferenced ?? [],
    createdAt,
  };
}

export function deleteLetter(id: string): void {
  db().prepare("DELETE FROM letters WHERE id = ?").run(id);
}

export function writeLettersMirror(): void {
  // Reverse-chronological in-app, but the file is most readable oldest-first
  // (so it reads as a sequence of letters received over time, like the log).
  const rows = db()
    .prepare(
      "SELECT id, body, fragments_referenced, created_at FROM letters ORDER BY created_at ASC"
    )
    .all() as Row[];
  const letters = rows.map(rowToLetter);
  const lines: string[] = [
    "# LETTERS.md",
    "",
    "Letters from Claude, written on request against the collection. Mirrored from `~/.commonplace/db.sqlite`; the database is the source of truth — edits made here will be overwritten.",
    "",
  ];
  for (const l of letters) {
    lines.push("---");
    lines.push("");
    lines.push(`### ${l.createdAt.slice(0, 10)}`);
    lines.push("");
    lines.push(l.body.trim());
    lines.push("");
  }
  writeFileSync(LETTERS_FILE, lines.join("\n"), "utf8");
}
