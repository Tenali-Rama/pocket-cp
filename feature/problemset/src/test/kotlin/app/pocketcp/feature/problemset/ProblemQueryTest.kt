package app.pocketcp.feature.problemset

import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import org.junit.Assert.assertEquals
import org.junit.Test

private fun problem(
    contestId: Int,
    index: String,
    name: String,
    rating: Int? = null,
    solved: Int? = null,
) = Problem(ProblemId(contestId, index), name, rating, tags = emptyList(), solvedCount = solved)

private val p1900a = problem(1900, "A", "Cover in Water", rating = 800, solved = 30_000)
private val p1900b = problem(1900, "B", "Laura and Operations", rating = 900, solved = 20_000)
private val p1900c = problem(1900, "C", "Anji's Binary Tree", rating = 1300)
private val p1900a1 = problem(1900, "A1", "water Splitting (easy)", solved = 10_000)
private val p190a = problem(190, "A", "Vasya and the Bus", rating = 1100, solved = 5_000)
private val p19005b = problem(19005, "B", "Laura Returns", rating = 1300, solved = 1)

/** In repository order: newest contest first, then by index. */
private val all = listOf(p19005b, p1900a, p1900a1, p1900b, p1900c, p190a)

class ProblemQueryTest {
    @Test
    fun blankQuery_matchesEverything() {
        assertEquals(all, search(""))
        assertEquals(all, search("   "))
    }

    @Test
    fun contestId_matchesEveryProblemWhoseDisplayStartsWithIt() {
        assertEquals(listOf(p19005b, p1900a, p1900a1, p1900b, p1900c), search("1900"))
        assertEquals(all, search("190"))
    }

    @Test
    fun problemId_isCaseInsensitive_trimmed_andAPrefix() {
        assertEquals(listOf(p1900a, p1900a1), search("1900a"))
        assertEquals(listOf(p1900a, p1900a1), search("  1900A "))
        assertEquals(listOf(p1900a1), search("1900A1"))
        assertEquals(listOf(p190a), search("190A"))
    }

    @Test
    fun idShapedQuery_neverMatchesNames() {
        val numbered = problem(5, "A", "1900 Coins")
        assertEquals(emptyList<Problem>(), searchIn(listOf(numbered), "1900"))
    }

    @Test
    fun otherText_isACaseInsensitiveNameSubstring() {
        assertEquals(listOf(p1900a, p1900a1), search("WATER"))
        assertEquals(listOf(p19005b, p1900b), search(" laura "))
        // Not id-shaped (letters before the digits, two letters, a space): name matches only.
        assertEquals(emptyList<Problem>(), search("A1900"))
        assertEquals(emptyList<Problem>(), search("1900AB"))
        assertEquals(emptyList<Problem>(), search("1900 A"))
    }

    @Test
    fun ratingAscending_putsUnratedLast_andKeepsRepositoryOrderOnTies() {
        assertEquals(
            listOf(p1900a, p1900b, p190a, p19005b, p1900c, p1900a1),
            search("", ProblemSort.RatingAscending),
        )
    }

    @Test
    fun ratingDescending_alsoPutsUnratedLast() {
        assertEquals(
            listOf(p19005b, p1900c, p190a, p1900b, p1900a, p1900a1),
            search("", ProblemSort.RatingDescending),
        )
    }

    @Test
    fun mostSolved_isDescending_withUnknownCountsLast() {
        assertEquals(
            listOf(p1900a, p1900b, p1900a1, p190a, p19005b, p1900c),
            search("", ProblemSort.MostSolved),
        )
    }

    @Test
    fun name_isCaseInsensitiveAlphabetical() {
        assertEquals(
            listOf(p1900c, p1900a, p1900b, p19005b, p190a, p1900a1),
            search("", ProblemSort.Name),
        )
    }

    @Test
    fun sortApplies_toTheMatchesOnly() {
        assertEquals(
            listOf(p19005b, p1900c, p1900b, p1900a, p1900a1),
            search("1900", ProblemSort.RatingDescending),
        )
    }

    private fun search(
        query: String,
        sort: ProblemSort = ProblemSort.Newest,
    ) = searchIn(all, query, sort)

    private fun searchIn(
        problems: List<Problem>,
        query: String,
        sort: ProblemSort = ProblemSort.Newest,
    ) = searchProblems(problems, ProblemQuery(query), sort)
}
