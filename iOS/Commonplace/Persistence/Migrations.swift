import Foundation
import SQLite3

enum Migrations {
    // Schema kept verbatim from the web app's src/lib/db.ts. Empty tables
    // (connections, claude_log_entries, settings) are preserved for parity
    // even though the iOS port doesn't surface them.
    static let schema = """
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
    """

    static func run(on db: OpaquePointer) throws {
        var error: UnsafeMutablePointer<CChar>?
        let rc = sqlite3_exec(db, schema, nil, nil, &error)
        if rc != SQLITE_OK {
            let msg = error.map { String(cString: $0) } ?? "unknown migration error"
            sqlite3_free(error)
            throw DatabaseError.openFailed(message: "schema migration failed: \(msg)")
        }
    }
}
