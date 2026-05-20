package com.commonplace.anthropic

import com.commonplace.data.repo.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The whole client we need: a non-streaming `messages.create` for the Ledger
 * + Letters + key test, and a streaming `messages.stream` for Marginalia.
 *
 * The web app uses the official @anthropic-ai/sdk; here we go direct because
 * the surface is two endpoints and SSE parsing fits in fifty lines.
 *
 * Endpoint and headers come from the Anthropic Messages API:
 *   POST https://api.anthropic.com/v1/messages
 *   Headers: x-api-key, anthropic-version, content-type
 */
class AnthropicService(private val settings: SettingsRepository) {

    class MissingApiKeyException : Exception("Anthropic API key is not configured.")
    class AnthropicHttpException(val status: Int, message: String) : Exception(message)

    @Serializable
    data class CreateRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String? = null,
        val messages: List<Message>,
        val stream: Boolean = false,
    )

    @Serializable
    data class Message(val role: String, val content: String)

    @Serializable
    private data class TextBlock(val type: String, val text: String? = null)

    @Serializable
    private data class CreateResponse(
        val id: String,
        val model: String? = null,
        val role: String? = null,
        val content: List<TextBlock> = emptyList(),
    )

    private val json = Json {
        ignoreUnknownKeys = true
        // Don't write `"system": null` into the request body when no system
        // prompt is supplied (test-key calls do this).
        explicitNulls = false
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        // Letters can be slow Opus calls; don't impose a tight read timeout.
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun createMessage(
        model: String,
        maxTokens: Int,
        system: String?,
        userMessage: String,
        overrideKey: String? = null,
    ): Result {
        val key = overrideKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: settings.getAnthropicKey()
            ?: throw MissingApiKeyException()

        val req = CreateRequest(
            model = model,
            maxTokens = maxTokens,
            system = system?.takeIf { it.isNotBlank() },
            messages = listOf(Message(role = "user", content = userMessage)),
            stream = false,
        )
        val body = json.encodeToString(CreateRequest.serializer(), req)
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(MESSAGES_URL)
            .post(body)
            .header("x-api-key", key)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("content-type", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AnthropicHttpException(response.code, parseError(text, response))
            }
            val parsed = json.decodeFromString(CreateResponse.serializer(), text)
            val combined = parsed.content
                .filter { it.type == "text" }
                .joinToString("") { it.text.orEmpty() }
                .trim()
            return Result(text = combined, model = parsed.model.orEmpty())
        }
    }

    /**
     * Streams a marginalia generation. Emits text deltas as they arrive. The
     * web app's contract is "happy path is just text; mid-stream errors are a
     * sentinel string". We keep that contract by emitting [Event] values that
     * the consumer interprets — the sentinel format isn't needed in-process,
     * but we preserve it conceptually as Event.Error.
     */
    fun streamMessage(
        model: String,
        maxTokens: Int,
        system: String,
        userMessage: String,
    ): Flow<Event> = flow {
        val key = settings.getAnthropicKey() ?: throw MissingApiKeyException()

        val req = CreateRequest(
            model = model,
            maxTokens = maxTokens,
            system = system,
            messages = listOf(Message(role = "user", content = userMessage)),
            stream = true,
        )
        val body = json.encodeToString(CreateRequest.serializer(), req)
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(MESSAGES_URL)
            .post(body)
            .header("x-api-key", key)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("content-type", "application/json")
            .header("accept", "text/event-stream")
            .build()

        val response = httpClient.newCall(request).execute()
        try {
            if (!response.isSuccessful) {
                val text = response.body?.string().orEmpty()
                throw AnthropicHttpException(response.code, parseError(text, response))
            }
            val source = response.body?.source()
                ?: throw IOException("Empty SSE body.")

            // SSE parser. Anthropic emits event/data line pairs separated by
            // a blank line. We only care about content_block_delta events
            // whose delta.type is "text_delta", and message_stop.
            var pendingEvent: String? = null
            var pendingData: StringBuilder? = null

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.isEmpty() -> {
                        // Dispatch buffered event.
                        val data = pendingData?.toString().orEmpty()
                        if (pendingEvent != null && data.isNotEmpty()) {
                            handleSseEvent(pendingEvent!!, data)?.let { emit(it) }
                        }
                        pendingEvent = null
                        pendingData = null
                    }
                    line.startsWith("event:") -> {
                        pendingEvent = line.removePrefix("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        val piece = line.removePrefix("data:").trimStart()
                        val buf = pendingData ?: StringBuilder().also { pendingData = it }
                        if (buf.isNotEmpty()) buf.append('\n')
                        buf.append(piece)
                    }
                    // Comments (lines starting with ":") and unknown fields ignored.
                }
            }
            emit(Event.Done)
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun handleSseEvent(event: String, data: String): Event? {
        if (event == "ping") return null
        if (event == "message_stop") return Event.Done
        if (event == "error") {
            val msg = runCatching {
                val obj = json.parseToJsonElement(data).jsonObject
                obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                    ?: obj["message"]?.jsonPrimitive?.contentOrNull
                    ?: data
            }.getOrDefault(data)
            return Event.Error(msg ?: "stream interrupted.")
        }
        if (event != "content_block_delta") return null

        return runCatching {
            val obj: JsonObject = json.parseToJsonElement(data).jsonObject
            val delta = obj["delta"]?.jsonObject ?: return@runCatching null
            val type = delta["type"]?.jsonPrimitive?.contentOrNull
            if (type != "text_delta") return@runCatching null
            val text = delta["text"]?.jsonPrimitive?.contentOrNull
            if (text.isNullOrEmpty()) null else Event.TextDelta(text)
        }.getOrNull()
    }

    private fun parseError(rawBody: String, response: Response): String {
        if (rawBody.isBlank()) return "request failed (${response.code})"
        return runCatching {
            val tree = json.parseToJsonElement(rawBody)
            val obj = tree.jsonObject
            obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                ?: obj["message"]?.jsonPrimitive?.contentOrNull
                ?: rawBody
        }.getOrDefault(rawBody)
    }

    sealed interface Event {
        data class TextDelta(val text: String) : Event
        data class Error(val message: String) : Event
        data object Done : Event
    }

    data class Result(val text: String, val model: String)

    companion object {
        private const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
