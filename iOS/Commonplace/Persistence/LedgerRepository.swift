import Foundation
import SQLite3

enum LedgerRepository {
    static func list() throws -> [LedgerEntry] {
        try Database.shared.read { db in
            let sql = "SELECT id, body, author, created_at FROM ledger_entries ORDER BY created_at ASC"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            var out: [LedgerEntry] = []
            while try stmt.step() {
                out.append(parse(stmt))
            }
            return out
        }
    }

    static func recent(limit: Int) throws -> [LedgerEntry] {
        try Database.shared.read { db in
            let sql = "SELECT id, body, author, created_at FROM ledger_entries ORDER BY created_at DESC LIMIT ?"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            try stmt.bindInt(1, Int64(limit))
            var out: [LedgerEntry] = []
            while try stmt.step() {
                out.append(parse(stmt))
            }
            return out.reversed()
        }
    }

    static func last() throws -> LedgerEntry? {
        try Database.shared.read { db in
            let sql = "SELECT id, body, author, created_at FROM ledger_entries ORDER BY created_at DESC LIMIT 1"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            if try stmt.step() {
                return parse(stmt)
            }
            return nil
        }
    }

    @discardableResult
    static func create(body: String, author: LedgerAuthor) throws -> LedgerEntry {
        let id = UUID().uuidString.lowercased()
        let createdAt = ISO8601DateFormatter.stableFormatter.string(from: Date())
        try Database.shared.write { db in
            let sql = "INSERT INTO ledger_entries (id, body, author, created_at) VALUES (?, ?, ?, ?)"
            try Statement.exec(db, sql) { stmt in
                try stmt.bindText(1, id)
                try stmt.bindText(2, body)
                try stmt.bindText(3, author.rawValue)
                try stmt.bindText(4, createdAt)
            }
        }
        return LedgerEntry(id: id, body: body, author: author, createdAt: createdAt)
    }

    static func update(id: String, body: String) throws {
        try Database.shared.write { db in
            try Statement.exec(db, "UPDATE ledger_entries SET body = ? WHERE id = ?") { stmt in
                try stmt.bindText(1, body)
                try stmt.bindText(2, id)
            }
        }
    }

    static func delete(id: String) throws {
        try Database.shared.write { db in
            try Statement.exec(db, "DELETE FROM ledger_entries WHERE id = ?") { stmt in
                try stmt.bindText(1, id)
            }
        }
    }

    /// Number of fragments created since the most recent Ledger entry. If
    /// there's no Ledger yet, returns the total fragment count.
    static func countFragmentsSinceLast() throws -> Int {
        try Database.shared.read { db in
            let last = try Self.lastDateLocked(db)
            let sql: String
            let bindLast: Bool
            if let last = last {
                _ = last
                sql = "SELECT COUNT(*) FROM fragments WHERE created_at > ?"
                bindLast = true
            } else {
                sql = "SELECT COUNT(*) FROM fragments"
                bindLast = false
            }
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            if bindLast, let last = last {
                try stmt.bindText(1, last)
            }
            if try stmt.step() {
                return Int(stmt.int(0))
            }
            return 0
        }
    }

    private static func lastDateLocked(_ db: OpaquePointer) throws -> String? {
        let sql = "SELECT created_at FROM ledger_entries ORDER BY created_at DESC LIMIT 1"
        let stmt = try Statement(db, sql)
        defer { stmt.finalize() }
        if try stmt.step() {
            return stmt.text(0)
        }
        return nil
    }

    private static func parse(_ stmt: Statement) -> LedgerEntry {
        let id = stmt.text(0) ?? ""
        let body = stmt.text(1) ?? ""
        let authorRaw = stmt.text(2) ?? "claude"
        let createdAt = stmt.text(3) ?? ""
        let author = LedgerAuthor(rawValue: authorRaw) ?? .claude
        return LedgerEntry(id: id, body: body, author: author, createdAt: createdAt)
    }
}
