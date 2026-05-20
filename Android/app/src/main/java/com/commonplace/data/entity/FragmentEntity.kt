package com.commonplace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mirrors the web app's `fragments` table from src/lib/db.ts.
 * `tags` is a JSON array of strings, stored as TEXT.
 * `embedding` is reserved (the web app deferred embeddings to v2 and never
 * implemented them, but the column is kept for schema parity).
 */
@Entity(tableName = "fragments")
data class FragmentEntity(
    @PrimaryKey val id: String,
    val body: String,
    val source: String?,
    val tags: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    val embedding: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FragmentEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
