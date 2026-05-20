package com.commonplace.ui.letters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commonplace.anthropic.AnthropicService
import com.commonplace.anthropic.LetterGenerator
import com.commonplace.data.Letter
import com.commonplace.data.repo.LetterRepository
import com.commonplace.data.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LettersViewModel(
    private val letters: LetterRepository,
    private val generator: LetterGenerator,
    private val settings: SettingsRepository,
) : ViewModel() {

    val items: StateFlow<List<Letter>> = letters.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasApiKey: StateFlow<Boolean> = settings.observeAnthropicKey()
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _composeState = MutableStateFlow<ComposeState>(ComposeState.Idle)
    val composeState: StateFlow<ComposeState> = _composeState

    fun compose() {
        if (_composeState.value == ComposeState.Writing) return
        viewModelScope.launch {
            _composeState.value = ComposeState.Writing
            _composeState.value = try {
                generator.generate()
                ComposeState.Idle
            } catch (e: AnthropicService.MissingApiKeyException) {
                ComposeState.Error("no API key set — visit Settings to add one.")
            } catch (e: AnthropicService.AnthropicHttpException) {
                ComposeState.Error(e.message ?: "request failed (${e.status}).")
            } catch (e: java.net.UnknownHostException) {
                ComposeState.Error("no internet — try again when you're connected.")
            } catch (e: Throwable) {
                ComposeState.Error(e.message ?: "unknown error.")
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { letters.delete(id) }
    }
}

sealed interface ComposeState {
    data object Idle : ComposeState
    data object Writing : ComposeState
    data class Error(val message: String) : ComposeState
}
