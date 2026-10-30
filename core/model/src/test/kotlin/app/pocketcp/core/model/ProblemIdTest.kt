package app.pocketcp.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProblemIdTest {
    @Test
    fun displaysContestIdThenIndex() {
        assertEquals("4A", ProblemId(4, "A").display)
        assertEquals("1234B1", ProblemId(1234, "B1").display)
    }

    @Test
    fun equalIdsAreEqualWithEqualHashCodes() {
        val a = ProblemId(4, "A")
        val b = ProblemId(4, "A")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun differentContestOrIndexIsDifferent() {
        assertNotEquals(ProblemId(4, "A"), ProblemId(4, "B"))
        assertNotEquals(ProblemId(4, "A"), ProblemId(5, "A"))
    }
}
