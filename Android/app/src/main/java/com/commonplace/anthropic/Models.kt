package com.commonplace.anthropic

/**
 * Pinned model identifiers, mirrored from src/lib/models.ts. If a model 404s
 * or behaves oddly, re-check the docs at https://platform.claude.com/docs —
 * don't trust training data on these strings.
 */
object Models {
    const val MARGINALIA_MODEL = "claude-sonnet-4-6"
    const val LETTERS_MODEL = "claude-opus-4-7"

    const val MARGINALIA_MAX_TOKENS = 400
    const val LEDGER_MAX_TOKENS = 400
    const val LETTERS_MAX_TOKENS = 800
}
