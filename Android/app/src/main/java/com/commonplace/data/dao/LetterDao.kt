package com.commonplace.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.commonplace.data.entity.LetterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterDao {

    @Query("SELECT * FROM letters ORDER BY created_at DESC")
    fun observeAll(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM letters ORDER BY created_at DESC")
    suspend fun listAllSnapshot(): List<LetterEntity>

    @Insert
    suspend fun insert(letter: LetterEntity)

    @Query("DELETE FROM letters WHERE id = :id")
    suspend fun delete(id: String)
}
