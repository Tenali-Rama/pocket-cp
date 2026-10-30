package app.pocketcp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** The app's Room database. Any schema change bumps `version` and adds a migration test (CLAUDE.md). */
@Database(
    entities = [ContestEntity::class, ProblemEntity::class, CacheMetaEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(TagsConverter::class)
abstract class PocketCpDatabase : RoomDatabase() {
    abstract fun contestDao(): ContestDao

    abstract fun problemDao(): ProblemDao

    abstract fun cacheMetaDao(): CacheMetaDao

    companion object {
        const val NAME = "pocketcp.db"
    }
}
