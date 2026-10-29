package app.pocketcp.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureEntryOrderTest {
    private val icon = ImageVector.Builder("test", 1.dp, 1.dp, 1f, 1f).build()

    private fun entry(
        route: String,
        order: Int,
    ): FeatureEntry =
        object : FeatureEntry {
            override val destination = TopLevelDestination(route, label = 0, icon = icon, order = order)

            override fun register(graph: NavGraphBuilder) = Unit
        }

    private fun routesOf(entries: List<FeatureEntry>) = entries.map { it.destination.route }

    @Test
    fun entriesAreSortedByAscendingOrder_notByContributionOrder() {
        val contributed = setOf(entry("settings", 40), entry("home", 0), entry("profile", 30), entry("contests", 10))

        assertEquals(listOf("home", "contests", "profile", "settings"), routesOf(contributed.inNavigationOrder()))
    }

    @Test
    fun equalOrders_areBrokenByRoute_soTheResultIsStable() {
        val first = listOf(entry("b", 5), entry("a", 5), entry("c", 1)).inNavigationOrder()
        val second = listOf(entry("a", 5), entry("c", 1), entry("b", 5)).inNavigationOrder()

        assertEquals(listOf("c", "a", "b"), routesOf(first))
        assertEquals(routesOf(first), routesOf(second))
    }

    @Test
    fun noEntries_givesAnEmptyList() {
        assertEquals(emptyList<FeatureEntry>(), emptySet<FeatureEntry>().inNavigationOrder())
    }

    @Test
    fun testTag_isNavPrefixedRoute() {
        assertEquals("nav_problemset", entry("problemset", 20).destination.testTag)
    }
}
