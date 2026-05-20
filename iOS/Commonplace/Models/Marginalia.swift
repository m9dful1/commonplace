import Foundation

struct Marginalia: Identifiable, Hashable, Sendable {
    let id: String
    let fragmentId: String
    let body: String
    let voice: String?
    let createdAt: String
}
