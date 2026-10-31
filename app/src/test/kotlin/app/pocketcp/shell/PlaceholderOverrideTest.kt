package app.pocketcp.shell

import androidx.navigation.NavGraphBuilder
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** A feature replaces its placeholder by binding an entry with the same route (ADR 0008). */
class PlaceholderOverrideTest {
    /** Placeholders for the shell tests' destinations other than Settings: Home, Contests, Problemset, Profile. */
    private val placeholders: Set<FeatureEntry> =
        shellTestEntries.filter { it.destination.route != "settings" }.toSet()

    private fun placeholder(route: String) = placeholders.single { it.destination.route == route }

    /** A real feature's entry for the placeholder [of]'s destination. */
    private class RealEntry(
        of: FeatureEntry,
    ) : FeatureEntry {
        override val destination: TopLevelDestination = of.destination

        override fun register(graph: NavGraphBuilder) = Unit
    }

    @Test
    fun placeholderModule_bindsOnlyProfile() {
        assertEquals(setOf(Placeholders.profile), PlaceholderModule.placeholders())
    }

    @Test
    fun realEntry_replacesThePlaceholderWithItsRoute() {
        val contests = RealEntry(placeholder("contests"))

        val result = (placeholders + contests).withoutReplacedPlaceholders()

        val forContests = result.filter { it.destination.route == "contests" }
        assertEquals(1, forContests.size)
        assertSame(contests, forContests.single())
        assertEquals(
            setOf(placeholder("home"), contests, placeholder("problemset"), placeholder("profile")),
            result.toSet(),
        )
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
        val home = RealEntry(placeholder("home"))
        val problemset = RealEntry(placeholder("problemset"))

        val result = (placeholders + home + problemset).withoutReplacedPlaceholders()

        assertEquals(4, result.size)
        assertEquals(setOf(home, placeholder("contests"), problemset, placeholder("profile")), result.toSet())
    }
}
