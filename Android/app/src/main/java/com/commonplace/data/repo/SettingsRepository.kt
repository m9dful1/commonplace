package com.commonplace.data.repo

import com.commonplace.data.dao.SettingsDao
import com.commonplace.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Mirrors src/lib/settings.ts: a single key/value table with the API key
 * stored under the same `anthropic_api_key` slot. The web app falls back to
 * a `.env.local` env var if the DB row is missing; here, the DB is the only
 * source.
 */
class SettingsRepository(private val dao: SettingsDao) {

    suspend fun getAnthropicKey(): String? =
        dao.getValue(ANTHROPIC_KEY)?.trim()?.takeIf { it.isNotEmpty() }

    fun observeAnthropicKey(): Flow<String?> = dao.observeValue(ANTHROPIC_KEY)

    suspend fun setAnthropicKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            dao.delete(ANTHROPIC_KEY)
        } else {
            dao.upsert(SettingsEntity(key = ANTHROPIC_KEY, value = trimmed))
        }
    }

    fun maskKey(key: String?): String {
        if (key.isNullOrEmpty()) return ""
        if (key.length <= 8) return "•".repeat(key.length)
        return key.take(4) + "•".repeat(key.length - 8) + key.takeLast(4)
    }

    suspend fun isWelcomeSeen(): Boolean =
        dao.getValue(WELCOME_SEEN) == "true"

    fun observeWelcomeSeen(): Flow<Boolean> =
        dao.observeValue(WELCOME_SEEN).map { it == "true" }

    suspend fun setWelcomeSeen() {
        dao.upsert(SettingsEntity(key = WELCOME_SEEN, value = "true"))
    }

    companion object {
        private const val ANTHROPIC_KEY = "anthropic_api_key"
        private const val WELCOME_SEEN = "welcome_seen"
    }
}
