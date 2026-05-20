package com.commonplace.data.repo

import com.commonplace.data.Fragment
import com.commonplace.data.dao.FragmentDao
import com.commonplace.data.encodeStringArray
import com.commonplace.data.entity.FragmentEntity
import com.commonplace.data.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class FragmentRepository(private val dao: FragmentDao) {

    fun observeAll(): Flow<List<Fragment>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observe(id: String): Flow<Fragment?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun get(id: String): Fragment? = dao.getById(id)?.toDomain()

    suspend fun create(body: String, source: String?, tags: List<String>): Fragment {
        val id = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        val cleanedSource = source?.trim()?.takeIf { it.isNotEmpty() }
        val entity = FragmentEntity(
            id = id,
            body = body,
            source = cleanedSource,
            tags = encodeStringArray(tags),
            createdAt = createdAt,
            embedding = null,
        )
        dao.insert(entity)
        return Fragment(
            id = id,
            body = body,
            source = cleanedSource,
            tags = tags,
            createdAt = createdAt,
        )
    }

    suspend fun listRecentExcluding(excludeId: String, limit: Int = 20): List<Fragment> =
        dao.listRecentExcluding(excludeId, limit).map { it.toDomain() }

    suspend fun listMostRecent(limit: Int): List<Fragment> =
        dao.listRecent(limit).map { it.toDomain() }

    /**
     * Mirrors src/lib/format.ts parseTagsInput — comma-separated, leading
     * `#` removed, blanks discarded.
     */
    fun parseTagsInput(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",")
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
    }
}
