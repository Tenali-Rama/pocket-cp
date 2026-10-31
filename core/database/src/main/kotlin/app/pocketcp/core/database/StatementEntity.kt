package app.pocketcp.core.database

import androidx.room.Entity

/**
 * One cached problem statement in one language, keyed like `ProblemId(contestId, index)` plus [language].
 * [language] is the `StatementLanguage` key, [document] the serialized statement that core/data writes, and
 * [formatVersion] the version of that serialization: rows of another version are stale and get refetched.
 */
@Entity(tableName = "statements", primaryKeys = ["contestId", "index", "language"])
data class StatementEntity(
    val contestId: Int,
    val index: String,
    val language: String,
    val document: String,
    val formatVersion: Int,
    val fetchedAtEpochMillis: Long,
)
