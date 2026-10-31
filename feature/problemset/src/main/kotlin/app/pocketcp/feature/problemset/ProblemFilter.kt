package app.pocketcp.feature.problemset

import androidx.compose.runtime.Immutable
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.Problem
import kotlin.math.roundToInt

/**
 * The problemset filters (spec §7.3). [tags]: a problem must carry every one (AND, as on Codeforces).
 * [ratingMin]..[ratingMax]: inclusive, in [RATING_STEP]s over [MIN_RATING]..[MAX_RATING]; unrated problems pass
 * only while the range is the full one. [divisions]: the problem's contest must be one of them; a contest that is
 * not cached has no known division, so its problems pass only while no division is selected. Empty means all.
 */
@Immutable
data class ProblemFilter(
    val tags: Set<String> = emptySet(),
    val ratingMin: Int = MIN_RATING,
    val ratingMax: Int = MAX_RATING,
    val divisions: Set<ContestDivision> = emptySet(),
) {
    /** Whether the rating range is narrower than the full one, which drops unrated problems. */
    val ratingNarrowed: Boolean get() = ratingMin > MIN_RATING || ratingMax < MAX_RATING

    /** The count on the filter button: each selected tag and division, and the rating range when narrowed. */
    val activeCount: Int get() = tags.size + divisions.size + if (ratingNarrowed) 1 else 0

    /** Whether [problem] passes, with [divisions] of each cached contest looked up in [divisionOf] by contest id. */
    fun matches(
        problem: Problem,
        divisionOf: Map<Int, ContestDivision>,
    ): Boolean =
        problem.tags.containsAll(tags) &&
            (!ratingNarrowed || problem.rating?.let { it in ratingMin..ratingMax } == true) &&
            (divisions.isEmpty() || divisionOf[problem.id.contestId] in divisions)

    companion object {
        const val MIN_RATING = 800
        const val MAX_RATING = 3500
        const val RATING_STEP = 100
        private val ALL_RATINGS = MIN_RATING..MAX_RATING

        /** A range snapped to [RATING_STEP]s inside [MIN_RATING]..[MAX_RATING], with min ≤ max. */
        fun snapRange(
            min: Int,
            max: Int,
        ): Pair<Int, Int> {
            fun snap(value: Int) = ((value.toDouble() / RATING_STEP).roundToInt() * RATING_STEP).coerceIn(ALL_RATINGS)
            val low = snap(min)
            val high = snap(max)
            return minOf(low, high) to maxOf(low, high)
        }
    }
}
