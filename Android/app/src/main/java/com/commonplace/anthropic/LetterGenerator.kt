package com.commonplace.anthropic

import com.commonplace.data.Letter
import com.commonplace.data.repo.FragmentRepository
import com.commonplace.data.repo.LedgerRepository
import com.commonplace.data.repo.LetterRepository
import com.commonplace.data.repo.MarginaliaRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Mirrors src/lib/lettersGenerate.ts. Opus-tier, non-streaming. The web app
 * passes ~30 fragments + last 2 Ledger entries + today's date.
 */
class LetterGenerator(
    private val anthropic: AnthropicService,
    private val fragments: FragmentRepository,
    private val marginalia: MarginaliaRepository,
    private val ledger: LedgerRepository,
    private val letters: LetterRepository,
) {

    suspend fun generate(): Letter {
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

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val userMessage = Prompts.buildLetterUserMessage(
            fragments = context,
            recentLedgerEntries = recentLedger,
            today = today,
        )

        val response = anthropic.createMessage(
            model = Models.LETTERS_MODEL,
            maxTokens = Models.LETTERS_MAX_TOKENS,
            system = Prompts.LETTERS_SYSTEM_PROMPT,
            userMessage = userMessage,
        )

        val text = response.text.trim()
        if (text.isEmpty()) {
            throw IllegalStateException("Letter generation returned empty body.")
        }

        return letters.create(
            body = text,
            fragmentsReferenced = recentFragments.map { it.id },
        )
    }

    companion object {
        private const val MAX_FRAGMENTS = 30
        private const val RECENT_LEDGER_COUNT = 2
    }
}
