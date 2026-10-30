package app.pocketcp.feature.problemset

import app.pocketcp.core.model.Problem

/** A problem id, whole (`1900A`, `1234B1`) or just its contest (`1900`): searched by `display` prefix. */
private val IdQuery = Regex("""^\d+[A-Za-z]\d*$|^\d+$""")

/**
 * What the search box matches. [text] is trimmed and compared ignoring case. An id-shaped query matches
 * problems whose `display` starts with it, so `1900` finds all of contest 1900 and `1900a` finds `1900A`;
 * anything else matches names containing it. A blank query matches everything.
 */
class ProblemQuery(
    text: String,
) {
    private val needle = text.trim()
    private val byId = IdQuery.matches(needle)

    fun matches(problem: Problem): Boolean =
        when {
            needle.isEmpty() -> true
            byId -> problem.id.display.startsWith(needle, ignoreCase = true)
            else -> problem.name.contains(needle, ignoreCase = true)
        }
}

/** The list's order. */
enum class ProblemSort {
    /** The repository's order: newest contest first, then by index. */
    Newest,
    RatingAscending,
    RatingDescending,
    MostSolved,
    Name,
    ;

    /** Whether this order reveals ratings, so it is not offered while ratings are hidden. */
    val usesRating: Boolean get() = this == RatingAscending || this == RatingDescending
}

/**
 * [problems] (in repository order) that match [query], in [sort] order. Unrated problems, and problems without a
 * solved count, sort last; ties keep the repository's order.
 */
internal fun searchProblems(
    problems: List<Problem>,
    query: ProblemQuery,
    sort: ProblemSort,
): List<Problem> {
    val matching = problems.filter(query::matches)
    return when (sort) {
        ProblemSort.Newest -> matching
        ProblemSort.RatingAscending -> matching.sortedWith(compareBy(nullsLast()) { it.rating })
        ProblemSort.RatingDescending -> matching.sortedWith(compareBy(nullsLast(reverseOrder())) { it.rating })
        ProblemSort.MostSolved -> matching.sortedWith(compareBy(nullsLast(reverseOrder())) { it.solvedCount })
        ProblemSort.Name -> matching.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
}
