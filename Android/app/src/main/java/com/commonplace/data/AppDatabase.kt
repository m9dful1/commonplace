package com.commonplace.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.commonplace.data.dao.FragmentDao
import com.commonplace.data.dao.LedgerDao
import com.commonplace.data.dao.LetterDao
import com.commonplace.data.dao.MarginaliaDao
import com.commonplace.data.dao.SettingsDao
import com.commonplace.data.entity.ClaudeLogEntryEntity
import com.commonplace.data.entity.ConnectionEntity
import com.commonplace.data.entity.FragmentEntity
import com.commonplace.data.entity.LedgerEntryEntity
import com.commonplace.data.entity.LetterEntity
import com.commonplace.data.entity.MarginaliaEntity
import com.commonplace.data.entity.SettingsEntity

/**
 * Schema mirrors src/lib/db.ts. The connections and claude_log_entries tables
 * exist for parity but have no DAO surfaces — same as the web app, where they
 * sit unused.
 */
@Database(
    entities = [
        FragmentEntity::class,
        MarginaliaEntity::class,
        LedgerEntryEntity::class,
        LetterEntity::class,
        ConnectionEntity::class,
        ClaudeLogEntryEntity::class,
        SettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fragmentDao(): FragmentDao
    abstract fun marginaliaDao(): MarginaliaDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun letterDao(): LetterDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DB_NAME = "commonplace.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
