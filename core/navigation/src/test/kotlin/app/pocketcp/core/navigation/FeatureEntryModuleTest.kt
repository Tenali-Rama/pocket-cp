package app.pocketcp.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.model.OptionalModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureEntryModuleTest {
    private val icon = ImageVector.Builder("test", 1.dp, 1.dp, 1f, 1f).build()

    private fun entry(
        route: String,
        module: OptionalModule?,
    ): FeatureEntry =
        object : FeatureEntry {
            override val destination = TopLevelDestination(route, label = 0, icon = icon, order = 0)
            override val module = module

            override fun register(graph: NavGraphBuilder) = Unit
        }

    private val entries =
        listOf(entry("home", module = null), entry("stats", OptionalModule.Stats), entry("ai", OptionalModule.Ai))

    private fun routesOf(entries: List<FeatureEntry>) = entries.map { it.destination.route }

    @Test
    fun module_defaultsToNull_soExistingEntriesAreCore() {
        val plain =
            object : FeatureEntry {
                override val destination = TopLevelDestination("home", label = 0, icon = icon, order = 0)

                override fun register(graph: NavGraphBuilder) = Unit
            }

        assertNull(plain.module)
    }

    @Test
    fun nothingDisabled_keepsEveryEntryInOrder() {
        assertEquals(listOf("home", "stats", "ai"), routesOf(entries.withoutDisabled(emptySet())))
    }

    @Test
    fun disabledModule_dropsOnlyItsEntry() {
        assertEquals(listOf("home", "ai"), routesOf(entries.withoutDisabled(setOf(OptionalModule.Stats))))
    }

    @Test
    fun coreEntries_surviveEveryModuleDisabled() {
        assertEquals(listOf("home"), routesOf(entries.withoutDisabled(OptionalModule.entries.toSet())))
    }
}
