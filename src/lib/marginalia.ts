import { randomUUID } from "node:crypto";
import { db } from "./db";
import type { Fragment } from "./fragments";

export type Marginalia = {
  id: string;
  fragmentId: string;
  body: string;
  voice: string | null;
  createdAt: string;
};

type Row = {
  id: string;
  fragment_id: string;
  body: string;
  voice: string | null;
  created_at: string;
};

function rowToMarginalia(row: Row): Marginalia {
  return {
    id: row.id,
    fragmentId: row.fragment_id,
    body: row.body,
    voice: row.voice,
    createdAt: row.created_at,
  };
}

export function listMarginaliaForFragment(fragmentId: string): Marginalia[] {
  const rows = db()
    .prepare(
      `SELECT id, fragment_id, body, voice, created_at
         FROM marginalia
         WHERE fragment_id = ?
         ORDER BY created_at ASC`
    )
    .all(fragmentId) as Row[];
  return rows.map(rowToMarginalia);
}

export function createMarginalia(input: {
  fragmentId: string;
  body: string;
  voice?: string | null;
}): Marginalia {
  const id = randomUUID();
  const createdAt = new Date().toISOString();
  db()
    .prepare(
      `INSERT INTO marginalia (id, fragment_id, body, voice, created_at)
       VALUES (?, ?, ?, ?, ?)`
    )
    .run(id, input.fragmentId, input.body, input.voice ?? null, createdAt);
  return {
    id,
    fragmentId: input.fragmentId,
    body: input.body,
    voice: input.voice ?? null,
    createdAt,
  };
}

export function listRecentFragmentsForContext(
  excludeId: string,
  limit = 20
): Pick<Fragment, "body" | "source" | "createdAt">[] {
  const rows = db()
    .prepare(
      `SELECT body, source, created_at
         FROM fragments
         WHERE id != ?
         ORDER BY created_at DESC
         LIMIT ?`
    )
    .all(excludeId, limit) as {
    body: string;
    source: string | null;
    created_at: string;
  }[];
  return rows.map((r) => ({
    body: r.body,
    source: r.source,
    createdAt: r.created_at,
  }));
}
