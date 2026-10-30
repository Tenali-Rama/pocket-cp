package app.pocketcp.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One cached contest. Fields mirror `core/model`'s `Contest`; core/data maps between the two.
 * [phase] is the `ContestPhase` name and [kind] the `ContestKind` name. [startEpochSeconds] is null when the judge
 * gives no start time. Rows cached before version 2 read `Unknown` as their [kind].
 */
@Entity(tableName = "contests")
data class ContestEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val phase: String,
    val startEpochSeconds: Long?,
    val durationSeconds: Long,
    val isGym: Boolean,
    @ColumnInfo(defaultValue = "'Unknown'") val kind: String,
)

/** One cached problemset problem, keyed like `ProblemId(contestId, index)`. */
@Entity(tableName = "problems", primaryKeys = ["contestId", "index"])
data class ProblemEntity(
    val contestId: Int,
    val index: String,
    val name: String,
    val rating: Int?,
    val tags: List<String>,
    val solvedCount: Int?,
)

/** When the dataset named [key] (one of [CacheKeys]) was last fetched in full. */
@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val key: String,
    val fetchedAtEpochMillis: Long,
)

/** The `cache_meta` keys: one per dataset that a DAO's `replaceAll` swaps. */
object CacheKeys {
    const val CONTESTS = "contests"
    const val GYM_CONTESTS = "contests_gym"
    const val PROBLEMSET = "problemset"

    fun contests(gym: Boolean): String = if (gym) GYM_CONTESTS else CONTESTS
}
