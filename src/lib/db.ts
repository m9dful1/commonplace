import Database from "better-sqlite3";
import { mkdirSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";

const DATA_DIR = join(homedir(), ".commonplace");
const DB_PATH = join(DATA_DIR, "db.sqlite");

mkdirSync(DATA_DIR, { recursive: true });

declare global {
  // eslint-disable-next-line no-var
  var __commonplaceDb: Database.Database | undefined;
}

function open(): Database.Database {
  const db = new Database(DB_PATH);
  db.pragma("journal_mode = WAL");
  db.pragma("foreign_keys = ON");
  migrate(db);
  return db;
}

function migrate(db: Database.Database) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS fragments (
      id          TEXT PRIMARY KEY,
      body        TEXT NOT NULL,
      source      TEXT,
      tags        TEXT,
      created_at  TEXT NOT NULL,
      embedding   BLOB
    );

    CREATE TABLE IF NOT EXISTS marginalia (
      id          TEXT PRIMARY KEY,
      fragment_id TEXT NOT NULL REFERENCES fragments(id) ON DELETE CASCADE,
      body        TEXT NOT NULL,
      voice       TEXT,
      created_at  TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS connections (
      id            TEXT PRIMARY KEY,
      fragment_a_id TEXT NOT NULL REFERENCES fragments(id) ON DELETE CASCADE,
      fragment_b_id TEXT NOT NULL REFERENCES fragments(id) ON DELETE CASCADE,
      reason        TEXT,
      strength      REAL,
      created_at    TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS ledger_entries (
      id         TEXT PRIMARY KEY,
      body       TEXT NOT NULL,
      author     TEXT NOT NULL,
      created_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS letters (
      id                   TEXT PRIMARY KEY,
      body                 TEXT NOT NULL,
      fragments_referenced TEXT,
      created_at           TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS claude_log_entries (
      id             TEXT PRIMARY KEY,
      body           TEXT NOT NULL,
      claude_context TEXT,
      created_at     TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS settings (
      key   TEXT PRIMARY KEY,
      value TEXT NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_fragments_created_at
      ON fragments(created_at DESC);
    CREATE INDEX IF NOT EXISTS idx_marginalia_fragment
      ON marginalia(fragment_id);
  `);
}

export function db(): Database.Database {
  if (!global.__commonplaceDb) {
    global.__commonplaceDb = open();
  }
  return global.__commonplaceDb;
}

export const DB_FILE_PATH = DB_PATH;
