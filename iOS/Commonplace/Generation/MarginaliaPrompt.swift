import Foundation

enum MarginaliaPrompt {
    /// System prompt — copied verbatim from the web app's
    /// `src/lib/marginaliaPrompt.ts`. Do not edit in isolation; if this needs
    /// to change, change it in both surfaces.
    static let system = """
    You are writing a marginal note on a fragment in someone's commonplace book — their personal anthology of things worth keeping. The tradition goes back centuries; people kept commonplace books to metabolize what they read and thought.

    Your note belongs in the margin. It is brief — one to three sentences. Favor connection or question over summary. If another fragment in their collection bears on this one, name it. If a word has an interesting etymology, you may share it. If the fragment makes a claim that seems dubious, you may say so kindly.

    Do not summarize what they wrote back to them. Do not praise them. Do not offer further help. Be the kind of marginal note someone might find written by a friend in the back of a used book — surprising, brief, generous, real.
    """

    static func userMessage(
        fragment: Fragment,
        recentFragments: [(body: String, source: String?, createdAt: String)],
        priorMarginalia: [Marginalia]
    ) -> String {
        var parts: [String] = []
        parts.append("The fragment:")
        parts.append("")
        parts.append(fragment.body.trimmingCharacters(in: .whitespacesAndNewlines))
        if let source = fragment.source, !source.isEmpty {
            parts.append("")
            parts.append("— \(source)")
        }

        if !recentFragments.isEmpty {
            parts.append("")
            parts.append("---")
            parts.append("")
            parts.append("For context, recent fragments from their collection:")
            parts.append("")
            for f in recentFragments {
                let trimmed = f.body.trimmingCharacters(in: .whitespacesAndNewlines)
                let head = (f.source.flatMap { $0.isEmpty ? nil : $0 }).map { "\(trimmed) — \($0)" } ?? trimmed
                parts.append("• \(head)")
            }
        }

        if !priorMarginalia.isEmpty {
            parts.append("")
            parts.append("---")
            parts.append("")
            parts.append("Marginalia already written on this fragment (don't repeat them; offer a different reading):")
            parts.append("")
            for m in priorMarginalia {
                parts.append("• \(m.body.trimmingCharacters(in: .whitespacesAndNewlines))")
            }
        }
        return parts.joined(separator: "\n")
    }
}
