package com.commonplace.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commonplace.anthropic.AnthropicService
import com.commonplace.anthropic.LedgerGenerator
import com.commonplace.data.LedgerEntry
import com.commonplace.data.repo.LedgerRepository
import com.commonplace.data.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(
    private val ledger: LedgerRepository,
    private val generator: LedgerGenerator,
    private val settings: SettingsRepository,
) : ViewModel() {

    val entries: StateFlow<List<LedgerEntry>> = ledger.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasApiKey: StateFlow<Boolean> = settings.observeAnthropicKey()
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _genState = MutableStateFlow<GenState>(GenState.Idle)
    val genState: StateFlow<GenState> = _genState

    fun generate() {
        if (_genState.value == GenState.Generating) return
        viewModelScope.launch {
            _genState.value = GenState.Generating
            _genState.value = try {
                when (val r = generator.generate()) {
                    is LedgerGenerator.Result.Pass -> GenState.Passed
                    is LedgerGenerator.Result.Created -> GenState.Idle
                }
            } catch (e: AnthropicService.MissingApiKeyException) {
                GenState.Error("no API key set — visit Settings to add one.")
            } catch (e: AnthropicService.AnthropicHttpException) {
                GenState.Error(e.message ?: "request failed (${e.status}).")
            } catch (e: java.net.UnknownHostException) {
                GenState.Error("no internet — try again when you're connected.")
            } catch (e: Throwable) {
                GenState.Error(e.message ?: "unknown error.")
            }
        }
    }

    fun dismissPass() {
        _genState.value = GenState.Idle
    }

    fun update(id: String, body: String) {
        viewModelScope.launch { ledger.update(id, body) }
    }

    fun delete(id: String) {
        viewModelScope.launch { ledger.delete(id) }
    }
}

sealed interface GenState {
    data object Idle : GenState
    data object Generating : GenState
    data object Passed : GenState
    data class Error(val message: String) : GenState
}
