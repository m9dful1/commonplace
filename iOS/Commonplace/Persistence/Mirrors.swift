import Foundation

/// Writes `LEDGER.md` and `LETTERS.md` into the app's Documents directory
/// from the SQLite source-of-truth. The web app mirrors these next to its
/// project root; on iOS the equivalent user-reachable spot is Documents
/// (browseable via the Files app).
enum Mirrors {
    static func writeLedgerMirror() {
        guard let entries = try? LedgerRepository.list() else { return }
        var out = """
        # LEDGER.md

        A journal of the collection itself — what's been gathered, what shape it's taking. Mirrored from the app's database; edits made in the app appear here, edits made here will be overwritten.

        """
        for e in entries {
            let date = String(e.createdAt.prefix(10))
            out += "\n---\n\n### \(date) — \(e.author.rawValue)\n\n\(e.body.trimmingCharacters(in: .whitespacesAndNewlines))\n"
        }
        try? out.write(to: AppPaths.ledgerMirrorURL(), atomically: true, encoding: .utf8)
    }

    static func writeLettersMirror() {
        guard let letters = try? LetterRepository.listOldestFirst() else { return }
        var out = """
        # LETTERS.md

        Letters from Claude, written on request against the collection. Mirrored from the app's database — the database is the source of truth; edits made here will be overwritten.

        """
        for l in letters {
            let date = String(l.createdAt.prefix(10))
            out += "\n---\n\n### \(date)\n\n\(l.body.trimmingCharacters(in: .whitespacesAndNewlines))\n"
        }
        try? out.write(to: AppPaths.lettersMirrorURL(), atomically: true, encoding: .utf8)
    }
}
