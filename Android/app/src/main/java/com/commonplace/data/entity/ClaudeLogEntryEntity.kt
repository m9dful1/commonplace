package com.commonplace.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Reserved for parity. The web app's closing entry was firm that runtime
 * Claude should not write into this table — log entries are reflective
 * artifacts written by Claudes with build context. Same rule applies here.
 */
@Entity(tableName = "claude_log_entries")
data class ClaudeLogEntryEntity(
    @PrimaryKey val id: String,
    val body: String,
    @ColumnInfo(name = "claude_context") val claudeContext: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
)
