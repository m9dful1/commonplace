package com.commonplace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "letters")
data class LetterEntity(
    @PrimaryKey val id: String,
    val body: String,
    @ColumnInfo(name = "fragments_referenced") val fragmentsReferenced: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
)
