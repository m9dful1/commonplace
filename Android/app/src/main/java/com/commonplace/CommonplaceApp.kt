package com.commonplace

import android.app.Application
import com.commonplace.anthropic.AnthropicService
import com.commonplace.anthropic.LedgerGenerator
import com.commonplace.anthropic.LetterGenerator
import com.commonplace.data.AppDatabase
import com.commonplace.data.repo.FragmentRepository
import com.commonplace.data.repo.LedgerRepository
import com.commonplace.data.repo.LetterRepository
import com.commonplace.data.repo.MarginaliaRepository
import com.commonplace.data.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Single graph of dependencies, constructed once. We keep it here rather than
 * pulling in Hilt or Koin — the surface is small enough that one class is
 * easier to read than a DI configuration.
 */
class CommonplaceApp : Application() {

    val applicationScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    val database: AppDatabase by lazy { AppDatabase.create(this) }

    val settings: SettingsRepository by lazy {
        SettingsRepository(database.settingsDao())
    }

    val anthropic: AnthropicService by lazy {
        AnthropicService(settings)
    }

    val fragments: FragmentRepository by lazy {
        FragmentRepository(database.fragmentDao())
    }

    val marginalia: MarginaliaRepository by lazy {
        MarginaliaRepository(database.marginaliaDao(), database.fragmentDao())
    }

    val ledger: LedgerRepository by lazy {
        LedgerRepository(database.ledgerDao(), database.fragmentDao())
    }

    val letters: LetterRepository by lazy {
        LetterRepository(database.letterDao())
    }

    val ledgerGenerator: LedgerGenerator by lazy {
        LedgerGenerator(anthropic, fragments, marginalia, ledger)
    }

    val letterGenerator: LetterGenerator by lazy {
        LetterGenerator(anthropic, fragments, marginalia, ledger, letters)
    }
}
