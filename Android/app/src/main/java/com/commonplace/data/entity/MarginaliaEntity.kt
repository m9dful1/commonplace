package com.commonplace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marginalia",
    foreignKeys = [
        ForeignKey(
            entity = FragmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["fragment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["fragment_id"], name = "idx_marginalia_fragment")],
)
data class MarginaliaEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fragment_id") val fragmentId: String,
    val body: String,
    val voice: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
)
