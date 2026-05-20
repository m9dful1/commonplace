package com.commonplace.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commonplace.anthropic.AnthropicService
import com.commonplace.anthropic.Models
import com.commonplace.data.repo.FragmentRepository
import com.commonplace.data.repo.LedgerRepository
import com.commonplace.data.repo.LetterRepository
import com.commonplace.data.repo.MarginaliaRepository
import com.commonplace.data.repo.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val anthropic: AnthropicService,
    private val fragments: FragmentRepository,
    private val marginalia: MarginaliaRepository,
    private val ledger: LedgerRepository,
    private val letters: LetterRepository,
) : ViewModel() {

    val savedKey: StateFlow<String?> = settings.observeAnthropicKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val maskedKey: StateFlow<String> = settings.observeAnthropicKey()
        .map { settings.maskKey(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState

    private val _saveFlash = MutableStateFlow(false)
    val saveFlash: StateFlow<Boolean> = _saveFlash

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    fun runTest(candidate: String) {
        val key = candidate.trim()
        if (key.isEmpty()) return
        viewModelScope.launch {
            _testState.value = TestState.Testing
            _testState.value = try {
                val result = anthropic.createMessage(
                    model = Models.MARGINALIA_MODEL,
                    maxTokens = 1,
                    system = null,
                    userMessage = "ok",
                    overrideKey = key,
                )
                TestState.Ok(result.model.ifEmpty { Models.MARGINALIA_MODEL })
            } catch (e: AnthropicService.AnthropicHttpException) {
                TestState.Err(e.message ?: "request failed (${e.status}).")
            } catch (e: java.net.UnknownHostException) {
                TestState.Err("no internet — try again when you're connected.")
            } catch (e: Throwable) {
                TestState.Err(e.message ?: "Unknown error.")
            }
        }
    }

    fun save(candidate: String, onComplete: () -> Unit) {
        val key = candidate.trim()
        if (key.isEmpty()) return
        viewModelScope.launch {
            settings.setAnthropicKey(key)
            _saveFlash.value = true
            onComplete()
            kotlinx.coroutines.delay(2000)
            _saveFlash.value = false
        }
    }

    fun suggestedExportFilename(): String {
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return "commonplace-$today.json"
    }

    fun exportTo(context: Context, uri: Uri) {
        viewModelScope.launch {
            _exportState.value = ExportState.Working
            _exportState.value = try {
                val payload = buildExportPayload()
                val ok = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                        true
                    } == true
                }
                if (ok) {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "JSON file"
                    ExportState.Done(name)
                } else {
                    ExportState.Error("could not open the file for writing.")
                }
            } catch (e: Throwable) {
                ExportState.Error(e.message ?: "unknown error.")
            }
        }
    }

    private suspend fun buildExportPayload(): String = withContext(Dispatchers.IO) {
        val frags = fragments.listMostRecent(Int.MAX_VALUE)
        val margs = marginalia.listAll()
        val ledgerEntries = ledger.listAll()
        val letterRows = letters.listAll()

        val obj: JsonObject = buildJsonObject {
            put("schema", JsonPrimitive("commonplace-export-v1"))
            put("exported_at", JsonPrimitive(java.time.Instant.now().toString()))
            put("fragments", buildJsonArray {
                for (f in frags) {
                    add(buildJsonObject {
                        put("id", JsonPrimitive(f.id))
                        put("body", JsonPrimitive(f.body))
                        put("source", JsonPrimitive(f.source))
                        put("tags", JsonArray(f.tags.map { JsonPrimitive(it) }))
                        put("created_at", JsonPrimitive(f.createdAt))
                    })
                }
            })
            put("marginalia", buildJsonArray {
                for (m in margs) {
                    add(buildJsonObject {
                        put("id", JsonPrimitive(m.id))
                        put("fragment_id", JsonPrimitive(m.fragmentId))
                        put("body", JsonPrimitive(m.body))
                        put("voice", JsonPrimitive(m.voice))
                        put("created_at", JsonPrimitive(m.createdAt))
                    })
                }
            })
            put("ledger_entries", buildJsonArray {
                for (e in ledgerEntries) {
                    add(buildJsonObject {
                        put("id", JsonPrimitive(e.id))
                        put("body", JsonPrimitive(e.body))
                        put("author", JsonPrimitive(e.author.raw))
                        put("created_at", JsonPrimitive(e.createdAt))
                    })
                }
            })
            put("letters", buildJsonArray {
                for (l in letterRows) {
                    add(buildJsonObject {
                        put("id", JsonPrimitive(l.id))
                        put("body", JsonPrimitive(l.body))
                        put("fragments_referenced", JsonArray(l.fragmentsReferenced.map { JsonPrimitive(it) }))
                        put("created_at", JsonPrimitive(l.createdAt))
                    })
                }
            })
        }
        // Indent for human-readable export.
        kotlinx.serialization.json.Json {
            prettyPrint = true
        }.encodeToString(JsonObject.serializer(), obj)
    }
}

sealed interface TestState {
    data object Idle : TestState
    data object Testing : TestState
    data class Ok(val model: String) : TestState
    data class Err(val message: String) : TestState
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Working : ExportState
    data class Done(val location: String) : ExportState
    data class Error(val message: String) : ExportState
}
