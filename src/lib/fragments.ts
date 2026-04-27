import { randomUUID } from "node:crypto";
import { db } from "./db";

export type Fragment = {
  id: string;
  body: string;
  source: string | null;
  tags: string[];
  createdAt: string;
};

type Row = {
  id: string;
  body: string;
  source: string | null;
  tags: string | null;
  created_at: string;
};

function rowToFragment(row: Row): Fragment {
  let tags: string[] = [];
  if (row.tags) {
    try {
      const parsed = JSON.parse(row.tags);
      if (Array.isArray(parsed)) tags = parsed.filter((t) => typeof t === "string");
    } catch {
      // tags column corrupt — fall back to empty
    }
  }
  return {
    id: row.id,
    body: row.body,
    source: row.source,
    tags,
    createdAt: row.created_at,
  };
}

export function getFragment(id: string): Fragment | null {
  const row = db()
    .prepare(
      "SELECT id, body, source, tags, created_at FROM fragments WHERE id = ?"
    )
    .get(id) as Row | undefined;
  return row ? rowToFragment(row) : null;
}

export function listFragments(): Fragment[] {
  const rows = db()
    .prepare(
      "SELECT id, body, source, tags, created_at FROM fragments ORDER BY created_at DESC"
    )
    .all() as Row[];
  return rows.map(rowToFragment);
}

export function createFragment(input: {
  body: string;
  source?: string | null;
  tags?: string[];
}): Fragment {
  const id = randomUUID();
  const createdAt = new Date().toISOString();
  const tags = input.tags && input.tags.length ? JSON.stringify(input.tags) : null;
  const source = input.source && input.source.trim() ? input.source.trim() : null;

  db()
    .prepare(
      `INSERT INTO fragments (id, body, source, tags, created_at)
       VALUES (?, ?, ?, ?, ?)`
    )
    .run(id, input.body, source, tags, createdAt);

  return {
    id,
    body: input.body,
    source,
    tags: input.tags ?? [],
    createdAt,
  };
}

export function parseTagsInput(raw: string | null | undefined): string[] {
  if (!raw) return [];
  return raw
    .split(",")
    .map((t) => t.trim().replace(/^#/, ""))
    .filter((t) => t.length > 0);
}
