package app.pocketcp.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Cached statements, one row per problem and language, written one at a time as they are fetched. */
@Dao
interface StatementDao {
    /** The cached statement of problem [contestId]/[index] in [language], or null; re-emits after each write. */
    @Query("SELECT * FROM statements WHERE contestId = :contestId AND `index` = :index AND language = :language")
    fun statement(
        contestId: Int,
        index: String,
        language: String,
    ): Flow<StatementEntity?>

    /** Inserts [entity], or replaces the row with the same problem and language. */
    @Upsert
    suspend fun upsert(entity: StatementEntity)

    /** The indices of [contestId]'s problems cached in [language] with [formatVersion], ascending. */
    @Query(
        "SELECT `index` FROM statements " +
            "WHERE contestId = :contestId AND language = :language AND formatVersion = :formatVersion " +
            "ORDER BY `index` ASC",
    )
    suspend fun cachedIndices(
        contestId: Int,
        language: String,
        formatVersion: Int,
    ): List<String>
}
