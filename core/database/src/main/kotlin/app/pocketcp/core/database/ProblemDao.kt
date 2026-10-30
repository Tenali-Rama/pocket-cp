package app.pocketcp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProblemDao {
    /** The cached problemset: newest contest first, then by problem index. */
    @Query("SELECT * FROM problems ORDER BY contestId DESC, `index` ASC")
    abstract fun problems(): Flow<List<ProblemEntity>>

    /** Replaces the whole problemset with [problems] and records [fetchedAtEpochMillis] under [CacheKeys.PROBLEMSET]. */
    @Transaction
    open suspend fun replaceAll(
        problems: List<ProblemEntity>,
        fetchedAtEpochMillis: Long,
    ) {
        deleteAll()
        insertAll(problems)
        upsertMeta(CacheMetaEntity(CacheKeys.PROBLEMSET, fetchedAtEpochMillis))
    }

    @Query("DELETE FROM problems")
    protected abstract suspend fun deleteAll()

    @Insert
    protected abstract suspend fun insertAll(problems: List<ProblemEntity>)

    @Upsert
    protected abstract suspend fun upsertMeta(meta: CacheMetaEntity)
}
