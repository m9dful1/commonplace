import Foundation
import SQLite3

enum LetterRepository {
    static func list() throws -> [Letter] {
        try Database.shared.read { db in
            let sql = "SELECT id, body, fragments_referenced, created_at FROM letters ORDER BY created_at DESC"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            var out: [Letter] = []
            while try stmt.step() {
                out.append(parse(stmt))
            }
            return out
        }
    }

    /// Same data as `list()` but oldest-first — used by the LETTERS.md mirror.
    static func listOldestFirst() throws -> [Letter] {
        try Database.shared.read { db in
            let sql = "SELECT id, body, fragments_referenced, created_at FROM letters ORDER BY created_at ASC"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            var out: [Letter] = []
            while try stmt.step() {
                out.append(parse(stmt))
            }
            return out
        }
    }

    @discardableResult
    static func create(body: String, fragmentsReferenced: [String]) throws -> Letter {
        let id = UUID().uuidString.lowercased()
        let createdAt = ISO8601DateFormatter.stableFormatter.string(from: Date())
        let refsJson: String? = fragmentsReferenced.isEmpty ? nil : encodeRefs(fragmentsReferenced)
        try Database.shared.write { db in
            let sql = "INSERT INTO letters (id, body, fragments_referenced, created_at) VALUES (?, ?, ?, ?)"
            try Statement.exec(db, sql) { stmt in
                try stmt.bindText(1, id)
                try stmt.bindText(2, body)
                try stmt.bindText(3, refsJson)
                try stmt.bindText(4, createdAt)
            }
        }
        return Letter(id: id, body: body, fragmentsReferenced: fragmentsReferenced, createdAt: createdAt)
    }

    static func delete(id: String) throws {
        try Database.shared.write { db in
            try Statement.exec(db, "DELETE FROM letters WHERE id = ?") { stmt in
                try stmt.bindText(1, id)
            }
        }
    }

    private static func parse(_ stmt: Statement) -> Letter {
        let id = stmt.text(0) ?? ""
        let body = stmt.text(1) ?? ""
        let refsRaw = stmt.text(2)
        let createdAt = stmt.text(3) ?? ""
        return Letter(id: id, body: body, fragmentsReferenced: decodeRefs(refsRaw), createdAt: createdAt)
    }

    private static func encodeRefs(_ refs: [String]) -> String? {
        guard let data = try? JSONSerialization.data(withJSONObject: refs, options: []) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func decodeRefs(_ raw: String?) -> [String] {
        guard let raw, let data = raw.data(using: .utf8) else { return [] }
        guard let any = try? JSONSerialization.jsonObject(with: data, options: []) else { return [] }
        guard let arr = any as? [Any] else { return [] }
        return arr.compactMap { $0 as? String }
    }
}
