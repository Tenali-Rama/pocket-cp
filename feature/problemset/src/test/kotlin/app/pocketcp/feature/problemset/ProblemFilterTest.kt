package app.pocketcp.feature.problemset

import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun problem(
    contestId: Int,
    index: String,
    rating: Int?,
    vararg tags: String,
) = Problem(ProblemId(contestId, index), "Problem $contestId$index", rating, tags.toList(), solvedCount = null)

private val p2001a = problem(2001, "A", 800, "math", "greedy")
private val p2001d = problem(2001, "D", 1600, "dp", "greedy", "math")
private val p2002b = problem(2002, "B", 1200, "greedy")
private val p2002c = problem(2002, "C", null, "math", "greedy")
private val p2003e = problem(2003, "E", 3500, "dp")

/** 2001 is Div. 2, 2002 Educational; contest 2003 is not cached. */
private val divisionOf = mapOf(2001 to ContestDivision.Div2, 2002 to ContestDivision.Educational)

private val all = listOf(p2001a, p2001d, p2002b, p2002c, p2003e)

class ProblemFilterTest {
    @Test
    fun defaultFilter_keepsEverything_includingUnratedAndUncachedContests() {
        assertEquals(all, filter(ProblemFilter()))
        assertEquals(0, ProblemFilter().activeCount)
        assertFalse(ProblemFilter().ratingNarrowed)
    }

    @Test
    fun tags_mustAllMatch() {
        assertEquals(listOf(p2001a, p2001d, p2002c), filter(ProblemFilter(tags = setOf("math"))))
        assertEquals(listOf(p2001a, p2001d, p2002c), filter(ProblemFilter(tags = setOf("math", "greedy"))))
        assertEquals(listOf(p2001d), filter(ProblemFilter(tags = setOf("math", "dp"))))
        assertEquals(emptyList<Problem>(), filter(ProblemFilter(tags = setOf("graphs"))))
    }

    @Test
    fun ratingRange_isInclusive_andDropsUnratedOnceNarrowed() {
        assertEquals(listOf(p2001d, p2002b), filter(ProblemFilter(ratingMin = 1200, ratingMax = 1600)))
        // Narrowing only one end still drops the unrated problem.
        assertEquals(listOf(p2001a, p2001d, p2002b), filter(ProblemFilter(ratingMax = 3400)))
        assertEquals(listOf(p2001d, p2002b, p2003e), filter(ProblemFilter(ratingMin = 900)))
        assertEquals(listOf(p2003e), filter(ProblemFilter(ratingMin = 3500)))
    }

    @Test
    fun divisions_lookUpTheContest_andDropUncachedContests() {
        assertEquals(listOf(p2001a, p2001d), filter(ProblemFilter(divisions = setOf(ContestDivision.Div2))))
        assertEquals(
            listOf(p2001a, p2001d, p2002b, p2002c),
            filter(ProblemFilter(divisions = setOf(ContestDivision.Div2, ContestDivision.Educational))),
        )
        assertEquals(emptyList<Problem>(), filter(ProblemFilter(divisions = setOf(ContestDivision.Div1))))
        // Without any cached contest, a division keeps nothing.
        val div2 = ProblemFilter(divisions = setOf(ContestDivision.Div2))
        assertEquals(emptyList<Problem>(), all.filter { div2.matches(it, emptyMap()) })
    }

    @Test
    fun filters_combine() {
        val filter =
            ProblemFilter(
                tags = setOf("greedy"),
                ratingMin = 1000,
                ratingMax = 2000,
                divisions = setOf(ContestDivision.Div2, ContestDivision.Educational),
            )
        assertEquals(listOf(p2001d, p2002b), filter(filter))
        assertEquals(4, filter.activeCount)
        assertTrue(filter.ratingNarrowed)
    }

    @Test
    fun filters_combineWithSearch_andSort() {
        val filter = ProblemFilter(tags = setOf("greedy"))
        assertEquals(
            listOf(p2002b, p2002c),
            searchProblems(all, ProblemQuery("2002"), ProblemSort.Newest, filter, divisionOf),
        )
        assertEquals(
            listOf(p2001d, p2002b, p2001a, p2002c),
            searchProblems(all, ProblemQuery(""), ProblemSort.RatingDescending, filter, divisionOf),
        )
    }

    @Test
    fun snapRange_roundsToSteps_clampsAndOrders() {
        assertEquals(1200 to 1600, ProblemFilter.snapRange(1180, 1649))
        assertEquals(800 to 3500, ProblemFilter.snapRange(0, 9000))
        assertEquals(1300 to 2000, ProblemFilter.snapRange(2000, 1300))
    }

    private fun filter(filter: ProblemFilter): List<Problem> = all.filter { filter.matches(it, divisionOf) }
}
