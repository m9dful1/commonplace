package com.commonplace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val body: String,
    val author: String, // "claude" or "user"
    @ColumnInfo(name = "created_at") val createdAt: String,
)
