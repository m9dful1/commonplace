package com.commonplace.data

import com.commonplace.data.entity.FragmentEntity
import com.commonplace.data.entity.LedgerEntryEntity
import com.commonplace.data.entity.LetterEntity
import com.commonplace.data.entity.MarginaliaEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/**
 * Domain types matching the web app's `src/lib` shapes (Fragment, Marginalia,
 * LedgerEntry, Letter). Kept distinct from the Room entities so the UI never
 * imports persistence types directly.
 */

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

data class Fragment(
    val id: String,
    val body: String,
    val source: String?,
    val tags: List<String>,
    val createdAt: String,
)

data class Marginalia(
    val id: String,
    val fragmentId: String,
    val body: String,
    val voice: String?,
    val createdAt: String,
)

enum class LedgerAuthor(val raw: String) {
    Claude("claude"), User("user");

    companion object {
        fun parse(raw: String): LedgerAuthor =
            if (raw == "user") User else Claude
    }
}

data class LedgerEntry(
    val id: String,
    val body: String,
    val author: LedgerAuthor,
    val createdAt: String,
)

data class Letter(
    val id: String,
    val body: String,
    val fragmentsReferenced: List<String>,
    val createdAt: String,
)

internal fun parseStringArray(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val tree = json.parseToJsonElement(raw)
        if (tree is JsonArray) {
            tree.mapNotNull { (it as? JsonPrimitive)?.contentOrNull() }
        } else emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}

internal fun encodeStringArray(values: List<String>): String? {
    if (values.isEmpty()) return null
    // JsonArray's toString() emits valid JSON; we don't need to round-trip
    // through encodeToString.
    return JsonArray(values.map { JsonPrimitive(it) }).toString()
}

private fun JsonPrimitive.contentOrNull(): String? =
    if (this.isString) this.content else null

fun FragmentEntity.toDomain(): Fragment = Fragment(
    id = id,
    body = body,
    source = source,
    tags = parseStringArray(tags),
    createdAt = createdAt,
)

fun MarginaliaEntity.toDomain(): Marginalia = Marginalia(
    id = id,
    fragmentId = fragmentId,
    body = body,
    voice = voice,
    createdAt = createdAt,
)

fun LedgerEntryEntity.toDomain(): LedgerEntry = LedgerEntry(
    id = id,
    body = body,
    author = LedgerAuthor.parse(author),
    createdAt = createdAt,
)

fun LetterEntity.toDomain(): Letter = Letter(
    id = id,
    body = body,
    fragmentsReferenced = parseStringArray(fragmentsReferenced),
    createdAt = createdAt,
)