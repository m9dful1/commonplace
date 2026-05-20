import Foundation
import SQLite3

enum LedgerService {
    enum GenerateResult {
        case created(LedgerEntry)
        case pass
    }

    private static let maxFragments = 15
    private static let recentLedgerCount = 2

    static func generate() async throws -> GenerateResult {
        guard let client = AnthropicClient(apiKey: SettingsStore.getAnthropicKey()) else {
            throw AnthropicError.missingApiKey
        }

        let context = try gatherContext()
        let userMessage = LedgerPrompt.userMessage(
            fragments: context.fragments,
            recentLedgerEntries: context.recentLedgerEntries
        )

        let (text, _) = try await client.sendMessage(
            model: AnthropicModels.marginalia,
            maxTokens: AnthropicModels.ledgerMaxTokens,
            system: LedgerPrompt.system,
            userMessage: userMessage
        )

        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if LedgerPrompt.isPassResponse(trimmed) {
            return .pass
        }
        let entry = try LedgerRepository.create(body: trimmed, author: .claude)
        Mirrors.writeLedgerMirror()
        return .created(entry)
    }

    /// Recent fragments + their marginalia + the recent Ledger entries.
    private static func gatherContext() throws -> (
        fragments: [LedgerPrompt.FragmentForLedger],
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

        let fragments: [LedgerPrompt.FragmentForLedger] = fragRows.map { row in
            LedgerPrompt.FragmentForLedger(
                body: row.body,
                source: row.source,
                createdAt: row.createdAt,
                marginalia: margMap[row.id] ?? []
            )
        }

        return (fragments, recent)
    }
}
