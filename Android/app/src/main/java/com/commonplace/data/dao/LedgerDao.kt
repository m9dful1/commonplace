package com.commonplace.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.commonplace.data.entity.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Query("SELECT * FROM ledger_entries ORDER BY created_at ASC")
    fun observeAll(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries ORDER BY created_at ASC")
    suspend fun listAllSnapshot(): List<LedgerEntryEntity>

    @Query(
        """
        SELECT * FROM ledger_entries
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    suspend fun listMostRecent(limit: Int): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entries ORDER BY created_at DESC LIMIT 1")
    suspend fun mostRecent(): LedgerEntryEntity?

    @Insert
    suspend fun insert(entry: LedgerEntryEntity)

    @Query("UPDATE ledger_entries SET body = :body WHERE id = :id")
    suspend fun updateBody(id: String, body: String)

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun delete(id: String)
}
