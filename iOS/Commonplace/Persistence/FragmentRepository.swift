import Foundation
import SQLite3

enum FragmentRepository {
    static func list() throws -> [Fragment] {
        try Database.shared.read { db in
            let sql = "SELECT id, body, source, tags, created_at FROM fragments ORDER BY created_at DESC"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            var out: [Fragment] = []
            while try stmt.step() {
                out.append(Self.parse(stmt))
            }
            return out
        }
    }

    static func get(id: String) throws -> Fragment? {
        try Database.shared.read { db in
            let sql = "SELECT id, body, source, tags, created_at FROM fragments WHERE id = ?"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            try stmt.bindText(1, id)
            if try stmt.step() {
                return Self.parse(stmt)
            }
            return nil
        }
    }

    @discardableResult
    static func create(body: String, source: String?, tags: [String]) throws -> Fragment {
        let id = UUID().uuidString.lowercased()
        let createdAt = ISO8601DateFormatter.stableFormatter.string(from: Date())
        let normalizedSource = source?.trimmingCharacters(in: .whitespacesAndNewlines)
        let storedSource: String? = (normalizedSource?.isEmpty == false) ? normalizedSource : nil
        let storedTags: String? = tags.isEmpty ? nil : Self.encodeTags(tags)

        try Database.shared.write { db in
            let sql = "INSERT INTO fragments (id, body, source, tags, created_at) VALUES (?, ?, ?, ?, ?)"
            try Statement.exec(db, sql) { stmt in
                try stmt.bindText(1, id)
                try stmt.bindText(2, body)
                try stmt.bindText(3, storedSource)
                try stmt.bindText(4, storedTags)
                try stmt.bindText(5, createdAt)
            }
        }
        return Fragment(id: id, body: body, source: storedSource, tags: tags, createdAt: createdAt)
    }

    static func delete(id: String) throws {
        try Database.shared.write { db in
            // ON DELETE CASCADE on the marginalia.fragment_id FK takes care
            // of the related notes; we only need to delete the fragment.
            try Statement.exec(db, "DELETE FROM fragments WHERE id = ?") { stmt in
                try stmt.bindText(1, id)
            }
        }
    }

    /// Recent fragments excluding `excludeId`, used as marginalia context.
    static func recentForContext(excludeId: String, limit: Int = 20) throws -> [(body: String, source: String?, createdAt: String)] {
        try Database.shared.read { db in
            let sql = "SELECT body, source, created_at FROM fragments WHERE id != ? ORDER BY created_at DESC LIMIT ?"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            try stmt.bindText(1, excludeId)
            try stmt.bindInt(2, Int64(limit))
            var out: [(body: String, source: String?, createdAt: String)] = []
            while try stmt.step() {
                let body = stmt.text(0) ?? ""
                let source = stmt.text(1)
                let created = stmt.text(2) ?? ""
                out.append((body, source, created))
            }
            return out
        }
    }

    private static func parse(_ stmt: Statement) -> Fragment {
        let id = stmt.text(0) ?? ""
        let body = stmt.text(1) ?? ""
        let source = stmt.text(2)
        let tagsRaw = stmt.text(3)
        let createdAt = stmt.text(4) ?? ""
        let tags = Self.decodeTags(tagsRaw)
        return Fragment(id: id, body: body, source: source, tags: tags, createdAt: createdAt)
    }

    private static func encodeTags(_ tags: [String]) -> String? {
        guard let data = try? JSONSerialization.data(withJSONObject: tags, options: []) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func decodeTags(_ raw: String?) -> [String] {
        guard let raw, let data = raw.data(using: .utf8) else { return [] }
        guard let any = try? JSONSerialization.jsonObject(with: data, options: []) else { return [] }
        guard let arr = any as? [Any] else { return [] }
        return arr.compactMap { $0 as? String }
    }
}

extension ISO8601DateFormatter {
    /// ISO 8601 with fractional seconds + Z, matching the JS Date#toISOString
    /// format used by the web app — keeps round-trip parity if/when a user's
    /// data ever moves between platforms.
    static let stableFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()
}
