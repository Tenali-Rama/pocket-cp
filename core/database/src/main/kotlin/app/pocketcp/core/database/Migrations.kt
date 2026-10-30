package app.pocketcp.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 2 adds `contests.kind`. Cached contests get `Unknown`, so both contest datasets also lose their
 * `cache_meta` row: the next read refetches them instead of serving kind-less rows for the stale window.
 */
internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `contests` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'Unknown'")
            db.execSQL(
                "DELETE FROM `cache_meta` WHERE `key` IN (?, ?)",
                arrayOf(CacheKeys.contests(gym = false), CacheKeys.contests(gym = true)),
            )
        }
    }
