package com.commonplace.anthropic

import com.commonplace.data.LedgerAuthor
import com.commonplace.data.LedgerEntry
import com.commonplace.data.repo.FragmentRepository
import com.commonplace.data.repo.LedgerRepository
import com.commonplace.data.repo.MarginaliaRepository

/**
 * Mirrors src/lib/ledgerGenerate.ts. Returns either a created entry or a
 * pass result. The Sonnet-tier model is used for the Ledger, same as the
 * web app — Opus is reserved for Letters.
 */
class LedgerGenerator(
    private val anthropic: AnthropicService,
    private val fragments: FragmentRepository,
    private val marginalia: MarginaliaRepository,
    private val ledger: LedgerRepository,
) {

    sealed interface Result {
        data class Created(val entry: LedgerEntry, val raw: String) : Result
        data class Pass(val raw: String) : Result
    }

    suspend fun generate(): Result {
        val recentLedger = ledger.listRecent(RECENT_LEDGER_COUNT)
        val recentFragments = fragments.listMostRecent(MAX_FRAGMENTS).reversed()

        val allMarginalia = marginalia.listAll()
        val byFragment = allMarginalia.groupBy { it.fragmentId }

        val context = recentFragments.map { f ->
            Prompts.LedgerFragmentContext(
                body = f.body,
                source = f.source,
                createdAt = f.createdAt,
                marginalia = byFragment[f.id]?.map { it.body }.orEmpty(),
            )
        }

        val userMessage = Prompts.buildLedgerUserMessage(
            fragments = context,
            recentLedgerEntries = recentLedger,
        )

        val response = anthropic.createMessage(
            model = Models.MARGINALIA_MODEL,
            maxTokens = Models.LEDGER_MAX_TOKENS,
            system = Prompts.LEDGER_SYSTEM_PROMPT,
            userMessage = userMessage,
        )

        val text = response.text.trim()

        if (Prompts.isPassResponse(text)) {
            return Result.Pass(raw = text)
        }

        val entry = ledger.create(body = text, author = LedgerAuthor.Claude)
        return Result.Created(entry = entry, raw = text)
    }

    companion object {
        private const val MAX_FRAGMENTS = 15
        private const val RECENT_LEDGER_COUNT = 2
    }
}
