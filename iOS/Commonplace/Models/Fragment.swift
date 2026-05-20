import Foundation

struct Fragment: Identifiable, Hashable, Sendable {
    let id: String
    let body: String
    let source: String?
    let tags: [String]
    let createdAt: String
}

enum TagsParser {
    static func parse(_ raw: String) -> [String] {
        raw.split(separator: ",")
            .map { tag -> String in
                var s = tag.trimmingCharacters(in: .whitespacesAndNewlines)
                if s.hasPrefix("#") { s.removeFirst() }
                return s
            }
            .filter { !$0.isEmpty }
    }
}
