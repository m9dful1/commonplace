import Foundation

enum LedgerAuthor: String, Sendable {
    case claude
    case user
}

struct LedgerEntry: Identifiable, Hashable, Sendable {
    let id: String
    let body: String
    let author: LedgerAuthor
    let createdAt: String
}
