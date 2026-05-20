import Foundation

enum LedgerPrompt {
    /// Verbatim from `src/lib/ledgerPrompt.ts`. The Phase 4.5 build entry in
    /// CLAUDE_LOG.md is the empirical record of how this prompt's pass gate
    /// was calibrated; do not casually edit it.
    static let system = """
    You are looking at a section of someone's commonplace book — fragments they've been collecting, with margin notes a previous Claude has left on some of them, and possibly one or two recent Ledger entries already on file.

    Your job is to decide, first, whether a new Ledger entry is warranted at all. Most of the time, the answer is no.

    **Step 1 — the pass gate. Apply this BEFORE drafting anything.**

    The gate has two cases.

    CASE A — there is no prior Ledger entry. Write a first entry as long as the collection has at least a handful of fragments worth characterizing (a few themes, a recurring source, or a recognizable shape, possibly with one or two outliers). Only return "pass" if the collection is so thin or scattered that there is genuinely nothing yet to mark — for example, fewer than three fragments, or fragments with no shared thread of any kind.

    CASE B — there is a prior Ledger entry. Write a new entry only if the shape of the collection has genuinely changed since the last entry. The shape has changed if and only if at least one of these is true:
    - a meaningfully new theme has emerged that the prior entry does not already name;
    - a new source or thinker has entered the collection;
    - a new tension, contradiction, or surprise has appeared that the prior entry does not mark.

    The shape has NOT changed (and you should return "pass") if any of the following hold:
    - the new fragments deepen, extend, or restate themes the prior entry already named — even if you could write a slightly different angle on them;
    - only one or two thin or off-topic fragments have been added (a household note, a passing thought, a single off-cluster observation) without reshaping the section;
    - the only thing you can offer is a meta-observation about how the cluster is "thickening", "winding down", or has "exhausted its momentum" — these are restatements, not new shape;
    - you would be saying essentially the same thing the prior entry already says, in different words.

    In Case B, pass is the correct answer most of the time. Do NOT write a paragraph and then append "pass". Do NOT hedge by writing an entry "in case it's useful". Either return a Ledger entry, or return only the word "pass". Never both.

    **Step 2 — only if the gate clears, write the entry.**

    Write a brief Ledger entry: a short paragraph noting what shape this section of the collection is taking. The Ledger is a journal of the collection itself, not of the person. Describe what they've been gathering — themes, recurring sources, specific concerns, tensions, surprises. Be specific: name the actual people they're quoting, the actual recurring concerns, not vague abstractions. Be brief: 2 to 4 sentences, never longer than a small paragraph.

    If a fragment doesn't fit the cluster, name it without forcing it to. Most sections of a commonplace book have one rough shape and one or two outliers. Honor both.

    Do not psychologize. Do not predict where they are going. Do not claim to know who they are. Do not quote their fragments back at length. The Ledger's only job is to mark what's accreting, the way someone might note what's been added to a shelf.
    """

    struct FragmentForLedger {
        let body: String
        let source: String?
        let createdAt: String
        let marginalia: [String]
    }

    static func userMessage(
        fragments: [FragmentForLedger],
        recentLedgerEntries: [(body: String, createdAt: String)]
    ) -> String {
        var parts: [String] = []
        if !recentLedgerEntries.isEmpty {
            parts.append("Recent Ledger entries (oldest first):")
            parts.append("")
            for e in recentLedgerEntries {
                let date = String(e.createdAt.prefix(10))
                parts.append("(\(date)) \(e.body.trimmingCharacters(in: .whitespacesAndNewlines))")
                parts.append("")
            }
            parts.append("---")
            parts.append("")
            parts.append("Recent fragments in the collection (oldest first; some predate the last Ledger entry, included for context):")
        } else {
            parts.append("Recent fragments in the collection (oldest first):")
        }
        parts.append("")

        for f in fragments {
            let date = String(f.createdAt.prefix(10))
            let trimmed = f.body.trimmingCharacters(in: .whitespacesAndNewlines)
            let head = (f.source.flatMap { $0.isEmpty ? nil : $0 }).map { "\(trimmed) — \($0)" } ?? trimmed
            parts.append("(\(date)) \(head)")
            for m in f.marginalia {
                parts.append("    margin: \(m.trimmingCharacters(in: .whitespacesAndNewlines))")
            }
            parts.append("")
        }
        return parts.joined(separator: "\n")
    }

    static func isPassResponse(_ text: String) -> Bool {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        // /^pass[.!\s]*$/i — matches `pass`, `pass.`, `pass!`, `pass   ` etc.
        guard trimmed.lowercased().hasPrefix("pass") else { return false }
        let rest = trimmed.dropFirst(4)
        for c in rest {
            if c.isWhitespace || c == "." || c == "!" { continue }
            return false
        }
        return true
    }
}
