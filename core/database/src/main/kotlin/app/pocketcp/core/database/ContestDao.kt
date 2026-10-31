package app.pocketcp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ContestDao {
    /** One dataset (regular or gym), newest start first; contests without a start time come last. */
    @Query("SELECT * FROM contests WHERE isGym = :gym ORDER BY startEpochSeconds DESC, id DESC")
    abstract fun contests(gym: Boolean): Flow<List<ContestEntity>>

    /**
     * The regular dataset's running contests, then at most [upcomingLimit] upcoming ones. Each group is earliest
     * start first, contests without a start last, ties by id. Finished and unknown phases are left out.
     * A negative [upcomingLimit] means no limit in SQLite; callers pass zero or more.
     */
    @Query(
        """
        SELECT * FROM contests
        WHERE isGym = 0 AND (
            phase = 'Running' OR id IN (
                SELECT id FROM contests
                WHERE isGym = 0 AND phase = 'Upcoming'
                ORDER BY startEpochSeconds IS NULL, startEpochSeconds, id
                LIMIT :upcomingLimit
            )
        )
        ORDER BY phase = 'Upcoming', startEpochSeconds IS NULL, startEpochSeconds, id
        """,
    )
    abstract fun activeContests(upcomingLimit: Int): Flow<List<ContestEntity>>

    /**
     * Replaces the regular or gym dataset with [contests] and records [fetchedAtEpochMillis] under
     * [CacheKeys.contests]. Contests missing from [contests] are removed; the other dataset is untouched.
     */
    @Transaction
    open suspend fun replaceAll(
        gym: Boolean,
        contests: List<ContestEntity>,
        fetchedAtEpochMillis: Long,
    ) {
        require(contests.all { it.isGym == gym }) { "every contest must have isGym = $gym" }
        deleteAll(gym)
        insertAll(contests)
        upsertMeta(CacheMetaEntity(CacheKeys.contests(gym), fetchedAtEpochMillis))
    }

    @Query("DELETE FROM contests WHERE isGym = :gym")
    protected abstract suspend fun deleteAll(gym: Boolean)

    @Insert
    protected abstract suspend fun insertAll(contests: List<ContestEntity>)

    @Upsert
    protected abstract suspend fun upsertMeta(meta: CacheMetaEntity)
}
