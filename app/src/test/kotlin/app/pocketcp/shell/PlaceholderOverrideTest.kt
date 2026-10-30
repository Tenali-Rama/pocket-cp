package app.pocketcp.shell

import androidx.navigation.NavGraphBuilder
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** A feature replaces its placeholder by binding an entry with the same route (ADR 0008). */
class PlaceholderOverrideTest {
    private val placeholders = PlaceholderModule.placeholders()

    /** A real feature's entry for the placeholder [of]'s destination. */
    private class RealEntry(
        of: FeatureEntry,
    ) : FeatureEntry {
        override val destination: TopLevelDestination = of.destination

        override fun register(graph: NavGraphBuilder) = Unit
    }

    @Test
    fun realEntry_replacesThePlaceholderWithItsRoute() {
        val contests = RealEntry(Placeholders.contests)

        val result = (placeholders + contests).withoutReplacedPlaceholders()

        val forContests = result.filter { it.destination.route == "contests" }
        assertEquals(1, forContests.size)
        assertSame(contests, forContests.single())
        assertEquals(setOf(Placeholders.home, contests, Placeholders.problemset, Placeholders.profile), result.toSet())
        assertEquals(4, result.size)
    }

    @Test
    fun noRealEntries_keepsEveryPlaceholder() {
        val result = placeholders.withoutReplacedPlaceholders()

        assertEquals(4, result.size)
        assertEquals(placeholders, result.toSet())
    }

    @Test
    fun twoRealEntries_eachReplaceOnlyTheirOwnPlaceholder() {
        val home = RealEntry(Placeholders.home)
        val problemset = RealEntry(Placeholders.problemset)

        val result = (placeholders + home + problemset).withoutReplacedPlaceholders()

        assertEquals(4, result.size)
        assertEquals(setOf(home, Placeholders.contests, problemset, Placeholders.profile), result.toSet())
    }
}
