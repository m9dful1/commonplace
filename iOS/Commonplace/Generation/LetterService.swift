import Foundation
import SQLite3

enum LetterService {
    private static let maxFragments = 30
    private static let recentLedgerCount = 2

    static func generate() async throws -> Letter {
        guard let client = AnthropicClient(apiKey: SettingsStore.getAnthropicKey()) else {
            throw AnthropicError.missingApiKey
        }

        let context = try gatherContext()
        let today = isoDate(Date())

        let userMessage = LetterPrompt.userMessage(
            fragments: context.fragments,
            recentLedgerEntries: context.recentLedgerEntries,
            today: today
        )

        let (text, _) = try await client.sendMessage(
            model: AnthropicModels.letters,
            maxTokens: AnthropicModels.lettersMaxTokens,
            system: LetterPrompt.system,
            userMessage: userMessage
        )

        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            throw AnthropicError.invalidResponse
        }

        let letter = try LetterRepository.create(body: trimmed, fragmentsReferenced: context.fragmentIds)
        Mirrors.writeLettersMirror()
        return letter
    }

    private static func gatherContext() throws -> (
        fragments: [LetterPrompt.FragmentForLetter],
        fragmentIds: [String],
        recentLedgerEntries: [(body: String, createdAt: String)]
    ) {
        let recent = try LedgerRepository.recent(limit: recentLedgerCount)
            .map { (body: $0.body, createdAt: $0.createdAt) }

        let fragRows: [(id: String, body: String, source: String?, createdAt: String)] = try Database.shared.read { db in
            let sql = "SELECT id, body, source, created_at FROM fragments ORDER BY created_at DESC LIMIT ?"
            let stmt = try Statement(db, sql)
            defer { stmt.finalize() }
            try stmt.bindInt(1, Int64(maxFragments))
            var rows: [(String, String, String?, String)] = []
            while try stmt.step() {
                rows.append((stmt.text(0) ?? "", stmt.text(1) ?? "", stmt.text(2), stmt.text(3) ?? ""))
            }
            return rows.reversed().map { ($0.0, $0.1, $0.2, $0.3) }
        }

        let margMap = try MarginaliaRepository.mapAllByFragment()

        let fragments: [LetterPrompt.FragmentForLetter] = fragRows.map { row in
            LetterPrompt.FragmentForLetter(
                body: row.body,
                source: row.source,
                createdAt: row.createdAt,
                marginalia: margMap[row.id] ?? []
            )
        }
        let ids = fragRows.map { $0.id }

        return (fragments, ids, recent)
    }

    private static func isoDate(_ date: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .iso8601)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }
}
