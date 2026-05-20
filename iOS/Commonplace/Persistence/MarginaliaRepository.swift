import Foundation
import SQLite3

enum MarginaliaRepository {
    static func list(forFragment fragmentId: String) throws -> [Marginalia] {
        try Database.shared.read { db in
            let sql = """
                SELECT id, fragment_id, body, voice, created_at
                FROM marginalia
                WHERE fragment_id = ?
                ORDER BY created_at ASC
            """
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            try stmt.bindText(1, fragmentId)
            var out: [Marginalia] = []
            while try stmt.step() {
                out.append(Marginalia(
                    id: stmt.text(0) ?? "",
                    fragmentId: stmt.text(1) ?? "",
                    body: stmt.text(2) ?? "",
                    voice: stmt.text(3),
                    createdAt: stmt.text(4) ?? ""
                ))
            }
            return out
        }
    }

    /// Returns a map of fragment_id -> [marginalia bodies in order].
    /// Used by Ledger and Letter context-gathering.
    static func mapAllByFragment() throws -> [String: [String]] {
        try Database.shared.read { db in
            let sql = "SELECT fragment_id, body, created_at FROM marginalia ORDER BY created_at ASC"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            var map: [String: [String]] = [:]
            while try stmt.step() {
                let fid = stmt.text(0) ?? ""
                let body = stmt.text(1) ?? ""
                map[fid, default: []].append(body)
            }
            return map
        }
    }

    @discardableResult
    static func create(fragmentId: String, body: String, voice: String? = nil) throws -> Marginalia {
        let id = UUID().uuidString.lowercased()
        let createdAt = ISO8601DateFormatter.stableFormatter.string(from: Date())
        try Database.shared.write { db in
            let sql = "INSERT INTO marginalia (id, fragment_id, body, voice, created_at) VALUES (?, ?, ?, ?, ?)"
            try Statement.exec(db, sql) { stmt in
                try stmt.bindText(1, id)
                try stmt.bindText(2, fragmentId)
                try stmt.bindText(3, body)
                try stmt.bindText(4, voice)
                try stmt.bindText(5, createdAt)
            }
        }
        return Marginalia(id: id, fragmentId: fragmentId, body: body, voice: voice, createdAt: createdAt)
    }
}
