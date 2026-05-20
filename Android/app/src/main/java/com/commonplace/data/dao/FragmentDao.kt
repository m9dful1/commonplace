package com.commonplace.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.commonplace.data.entity.FragmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FragmentDao {

    @Query("SELECT * FROM fragments ORDER BY created_at DESC")
    fun observeAll(): Flow<List<FragmentEntity>>

    @Query("SELECT * FROM fragments WHERE id = :id")
    suspend fun getById(id: String): FragmentEntity?

    @Query("SELECT * FROM fragments WHERE id = :id")
    fun observeById(id: String): Flow<FragmentEntity?>

    @Insert
    suspend fun insert(fragment: FragmentEntity)

    @Query(
        """
        SELECT * FROM fragments
        WHERE id != :excludeId
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    suspend fun listRecentExcluding(excludeId: String, limit: Int): List<FragmentEntity>

    @Query("SELECT * FROM fragments ORDER BY created_at DESC LIMIT :limit")
    suspend fun listRecent(limit: Int): List<FragmentEntity>

    @Query("SELECT COUNT(*) FROM fragments")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM fragments WHERE created_at > :since")
    suspend fun countSince(since: String): Int
}
