package app.pocketcp.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class ContestDivisionTest {
    @Test
    fun `each division is read from a real contest name`() {
        val names =
            mapOf(
                "Codeforces Round 1127 (Div. 1, Based on Mexican OI 2026)" to ContestDivision.Div1,
                "Codeforces Round 1130 (Div. 2)" to ContestDivision.Div2,
                "Codeforces Round 1131 (Div. 3)" to ContestDivision.Div3,
                "Codeforces Round 993 (Div. 4)" to ContestDivision.Div4,
                "WorldQuant Round 1 (Codeforces Round 1128, Div. 1 + Div. 2)" to ContestDivision.Div1And2,
                "Educational Codeforces Round 195 (Rated for Div. 2)" to ContestDivision.Educational,
                "Codeforces Global Round 27" to ContestDivision.Global,
                "Kotlin Heroes: Episode 15" to ContestDivision.Other,
            )

        names.forEach { (name, division) -> assertEquals(name, division, ContestDivision.of(name)) }
        assertEquals(ContestDivision.entries.toSet(), names.values.toSet())
    }

    @Test
    fun `a combined round is Div1And2, also written Div 1 + 2`() {
        assertEquals(ContestDivision.Div1And2, ContestDivision.of("Pinely Round 4 (Div. 1 + Div. 2)"))
        assertEquals(ContestDivision.Div1And2, ContestDivision.of("Codeforces Round 1000 (Div. 1 + 2)"))
    }

    @Test
    fun `Educational and Global win over a stated division`() {
        assertEquals(ContestDivision.Educational, ContestDivision.of("Educational Round 170 (Rated for Div. 2)"))
        assertEquals(ContestDivision.Global, ContestDivision.of("Codeforces Global Round 28 (Div. 1 + Div. 2)"))
    }

    @Test
    fun `a gym name is Other`() {
        assertEquals(
            ContestDivision.Other,
            ContestDivision.of("2017-2018 ACM-ICPC, NEERC, Southern Subregional Contest"),
        )
    }

    @Test
    fun `a contest derives its division from its name and kind defaults to Unknown`() {
        val contest =
            Contest(ContestId(1), "Codeforces Round 1131 (Div. 3)", ContestPhase.Finished, null, Duration.ZERO, false)

        assertEquals(ContestDivision.Div3, contest.division)
        assertEquals(ContestKind.Unknown, contest.kind)
    }
}
