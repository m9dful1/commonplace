import Foundation

// Pinned in one place, mirroring src/lib/models.ts in the web app.
// Verified against https://platform.claude.com/docs/en/about-claude/models/overview
// at the time of writing. If a model 404s or behaves oddly, re-check the docs —
// don't trust your training data on these strings.

enum AnthropicModels {
    static let marginalia = "claude-sonnet-4-6"
    static let letters = "claude-opus-4-7"

    static let marginaliaMaxTokens = 400
    static let ledgerMaxTokens = 400
    static let lettersMaxTokens = 800
}
