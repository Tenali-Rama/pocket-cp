package app.pocketcp.core.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface CacheMetaDao {
    /** When the dataset [key] (one of [CacheKeys]) was last fetched, or null if it never was. */
    @Query("SELECT fetchedAtEpochMillis FROM cache_meta WHERE `key` = :key")
    suspend fun fetchedAt(key: String): Long?
}
