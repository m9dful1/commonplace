package com.commonplace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reserved table for parity with the web app's schema. The connections feature
 * was deferred to v2 in the original spec and never wired up; the table exists
 * so a future port could surface it without a migration.
 */
@Entity(
    tableName = "connections",
    foreignKeys = [
        ForeignKey(
            entity = FragmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["fragment_a_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FragmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["fragment_b_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["fragment_a_id"], name = "idx_connections_fragment_a"),
        Index(value = ["fragment_b_id"], name = "idx_connections_fragment_b"),
    ],
)
data class ConnectionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "fragment_a_id") val fragmentAId: String,
    @ColumnInfo(name = "fragment_b_id") val fragmentBId: String,
    val reason: String?,
    val strength: Double?,
    @ColumnInfo(name = "created_at") val createdAt: String,
)
