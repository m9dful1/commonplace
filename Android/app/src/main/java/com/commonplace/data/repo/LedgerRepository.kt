package com.commonplace.data.repo

import com.commonplace.data.LedgerAuthor
import com.commonplace.data.LedgerEntry
import com.commonplace.data.dao.FragmentDao
import com.commonplace.data.dao.LedgerDao
import com.commonplace.data.entity.LedgerEntryEntity
import com.commonplace.data.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class LedgerRepository(
    private val dao: LedgerDao,
    private val fragmentDao: FragmentDao,
) {

    fun observeAll(): Flow<List<LedgerEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun listAll(): List<LedgerEntry> =
        dao.listAllSnapshot().map { it.toDomain() }

    /**
     * Mirrors src/lib/ledger.ts listRecentLedgerEntries: take the most recent
     * `limit`, then reverse so they read oldest-first (the order Claude
     * expects in context).
     */
    suspend fun listRecent(limit: Int): List<LedgerEntry> =
        dao.listMostRecent(limit).reversed().map { it.toDomain() }

    suspend fun mostRecent(): LedgerEntry? = dao.mostRecent()?.toDomain()

    suspend fun create(body: String, author: LedgerAuthor): LedgerEntry {
        val id = UUID.randomUUID().toString()
        val createdAt = Instant.now().toString()
        dao.insert(
            LedgerEntryEntity(
                id = id,
                body = body,
                author = author.raw,
                createdAt = createdAt,
            )
        )
        return LedgerEntry(id = id, body = body, author = author, createdAt = createdAt)
    }

    suspend fun update(id: String, body: String) {
        dao.updateBody(id, body)
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    /**
     * Mirrors src/lib/ledger.ts countFragmentsSinceLastLedger.
     */
    suspend fun fragmentsSinceLast(): Int {
        val last = dao.mostRecent()
        return if (last == null) fragmentDao.countAll()
        else fragmentDao.countSince(last.createdAt)
    }
}
