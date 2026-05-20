package com.commonplace.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.commonplace.data.entity.MarginaliaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarginaliaDao {

    @Query(
        """
        SELECT * FROM marginalia
        WHERE fragment_id = :fragmentId
        ORDER BY created_at ASC
        """
    )
    fun observeForFragment(fragmentId: String): Flow<List<MarginaliaEntity>>

    @Query(
        """
        SELECT * FROM marginalia
        WHERE fragment_id = :fragmentId
        ORDER BY created_at ASC
        """
    )
    suspend fun listForFragment(fragmentId: String): List<MarginaliaEntity>

    @Query("SELECT * FROM marginalia ORDER BY created_at ASC")
    suspend fun listAll(): List<MarginaliaEntity>

    @Insert
    suspend fun insert(marginalia: MarginaliaEntity)
}
