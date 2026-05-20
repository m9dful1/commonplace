import Foundation

struct Letter: Identifiable, Hashable, Sendable {
    let id: String
    let body: String
    let fragmentsReferenced: [String]
    let createdAt: String
}
