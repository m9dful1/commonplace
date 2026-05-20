package com.commonplace.ui.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commonplace.CommonplaceApp
import com.commonplace.anthropic.LedgerGenerator
import com.commonplace.data.Fragment
import com.commonplace.data.repo.FragmentRepository
import com.commonplace.data.repo.LedgerRepository
import com.commonplace.data.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs FragmentsListScreen. Holds the reverse-chronological flow of
 * fragments and the "save fragment" command, including the fire-and-forget
 * Ledger trigger after every fifth fragment (mirror of src/app/actions.ts).
 */
class FragmentsViewModel(
    private val fragments: FragmentRepository,
    private val ledger: LedgerRepository,
    private val ledgerGenerator: LedgerGenerator,
    private val settings: SettingsRepository,
    private val app: CommonplaceApp,
) : ViewModel() {

    val items: StateFlow<List<Fragment>> = fragments.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(body: String, source: String?, tagsRaw: String?) {
        if (body.isBlank()) return
        val tags = fragments.parseTagsInput(tagsRaw)
        viewModelScope.launch {
            fragments.create(body = body.trim(), source = source, tags = tags)
            maybeTriggerLedger()
        }
    }

    private fun maybeTriggerLedger() {
        // Fire-and-forget on the application scope so a screen-leaving lifecycle
        // doesn't kill the call. Mirrors the web app's `void generateLedgerEntry()`.
        app.applicationScope.launch {
            if (settings.getAnthropicKey() == null) return@launch
            val n = ledger.fragmentsSinceLast()
            if (n < LEDGER_TRIGGER_THRESHOLD) return@launch
            runCatching { ledgerGenerator.generate() }
                .onFailure { /* same as the web's console.error — drop, the manual trigger covers us */ }
        }
    }

    companion object {
        private const val LEDGER_TRIGGER_THRESHOLD = 5
    }
}

class FragmentDetailViewModel(
    private val fragments: FragmentRepository,
    private val marginaliaRepo: com.commonplace.data.repo.MarginaliaRepository,
    private val anthropic: com.commonplace.anthropic.AnthropicService,
    private val settings: SettingsRepository,
    val fragmentId: String,
) : ViewModel() {

    val fragment: StateFlow<Fragment?> = fragments.observe(fragmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val marginalia: StateFlow<List<com.commonplace.data.Marginalia>> =
        marginaliaRepo.observeForFragment(fragmentId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasApiKey: StateFlow<Boolean> =
        settings.observeAnthropicKey()
            .map { !it.isNullOrBlank() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _stream = MutableStateFlow<MarginaliaStreamState>(MarginaliaStreamState.Idle)
    val stream: StateFlow<MarginaliaStreamState> = _stream

    fun requestMarginalia() {
        if (_stream.value is MarginaliaStreamState.Loading ||
            _stream.value is MarginaliaStreamState.Streaming
        ) return

        viewModelScope.launch {
            _stream.value = MarginaliaStreamState.Loading
            val key = settings.getAnthropicKey()
            if (key.isNullOrBlank()) {
                _stream.value = MarginaliaStreamState.Error(
                    message = "no API key set — visit Settings to add one.",
                    partial = null,
                )
                return@launch
            }

            val frag = fragments.get(fragmentId) ?: run {
                _stream.value = MarginaliaStreamState.Error(
                    message = "fragment not found.",
                    partial = null,
                )
                return@launch
            }

            val priorMarginalia = marginaliaRepo.listForFragment(fragmentId)
            val recent = fragments.listRecentExcluding(fragmentId, 20)

            val userMessage = com.commonplace.anthropic.Prompts.buildMarginaliaUserMessage(
                fragment = frag,
                recentFragments = recent,
                priorMarginalia = priorMarginalia,
            )

            val collected = StringBuilder()
            var sawError = false
            try {
                anthropic.streamMessage(
                    model = com.commonplace.anthropic.Models.MARGINALIA_MODEL,
                    maxTokens = com.commonplace.anthropic.Models.MARGINALIA_MAX_TOKENS,
                    system = com.commonplace.anthropic.Prompts.MARGINALIA_SYSTEM_PROMPT,
                    userMessage = userMessage,
                ).collect { event ->
                    if (sawError) return@collect
                    when (event) {
                        is com.commonplace.anthropic.AnthropicService.Event.TextDelta -> {
                            collected.append(event.text)
                            _stream.value = MarginaliaStreamState.Streaming(collected.toString())
                        }
                        is com.commonplace.anthropic.AnthropicService.Event.Error -> {
                            sawError = true
                            _stream.value = MarginaliaStreamState.Error(
                                message = event.message,
                                partial = collected.toString().trim().ifEmpty { null },
                            )
                        }
                        com.commonplace.anthropic.AnthropicService.Event.Done -> Unit
                    }
                }
            } catch (e: com.commonplace.anthropic.AnthropicService.MissingApiKeyException) {
                _stream.value = MarginaliaStreamState.Error(
                    message = "no API key set — visit Settings to add one.",
                    partial = null,
                )
                return@launch
            } catch (e: java.net.UnknownHostException) {
                _stream.value = MarginaliaStreamState.Error(
                    message = "no internet — try again when you're connected.",
                    partial = collected.toString().trim().ifEmpty { null },
                )
                return@launch
            } catch (e: Throwable) {
                _stream.value = MarginaliaStreamState.Error(
                    message = e.message ?: "request failed.",
                    partial = collected.toString().trim().ifEmpty { null },
                )
                return@launch
            }

            if (sawError) return@launch

            val finalText = collected.toString().trim()
            if (finalText.isNotEmpty()) {
                marginaliaRepo.create(fragmentId = fragmentId, body = finalText)
            }
            _stream.value = MarginaliaStreamState.Idle
        }
    }

    fun clearError() {
        _stream.value = MarginaliaStreamState.Idle
    }
}

sealed interface MarginaliaStreamState {
    data object Idle : MarginaliaStreamState
    data object Loading : MarginaliaStreamState
    data class Streaming(val text: String) : MarginaliaStreamState
    data class Error(val message: String, val partial: String?) : MarginaliaStreamState
}
