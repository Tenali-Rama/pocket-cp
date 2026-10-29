package app.pocketcp.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.NavigationType
import app.pocketcp.core.navigation.FeatureEntry

/**
 * The app shell: the [entries]' destinations in a bottom bar (compact width) or a navigation rail (wider),
 * next to a nav host holding every entry's screens. The first entry is the start destination; [entries] must
 * already be in navigation order. [windowInsets] are the system bars the chrome and content keep clear of.
 */
@Composable
fun PocketCpShell(
    entries: List<FeatureEntry>,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.systemBars,
    navController: NavHostController = rememberNavController(),
) {
    val start = entries.firstOrNull() ?: return
    val destinations = remember(entries) { entries.map { it.destination } }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val hierarchy = backStackEntry?.destination?.hierarchy
    val selectedRoute = destinations.firstOrNull { d -> hierarchy.orEmpty().any { it.route == d.route } }?.route
    val onSelect = remember(navController) { { route: String -> navController.navigateToTopLevel(route) } }
    // Movable, so screen state survives the switch between bottom bar and rail on resize or rotation.
    val navHost =
        remember(navController, entries) {
            movableContentOf { contentModifier: Modifier ->
                NavHost(navController, startDestination = start.destination.route, modifier = contentModifier) {
                    entries.forEach { it.register(this) }
                }
            }
        }
    when (LocalWindowLayout.current.navigationType) {
        NavigationType.BottomBar ->
            Column(modifier) {
                navHost(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                )
                ShellBottomBar(
                    destinations,
                    selectedRoute,
                    onSelect,
                    windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                )
            }
        NavigationType.Rail ->
            Row(modifier) {
                ShellRail(destinations, selectedRoute, onSelect, windowInsets.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                navHost(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)),
                )
            }
    }
}

/** Switches top-level destination: one copy of each on the stack, Home at the bottom, each tab's state saved. */
private fun NavController.navigateToTopLevel(route: String) =
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
