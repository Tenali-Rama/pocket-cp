package app.pocketcp.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.NavigationType
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination

/**
 * The app shell: the [entries]' destinations in a bottom bar (compact width) or a navigation rail (wider),
 * next to a nav host holding every entry's screens. The first entry is the start destination; [entries] must
 * already be in navigation order. [windowInsets] are the system bars the chrome and content keep clear of.
 * While [offline], an [OfflineBanner] sits at the top of the content, under the status bar and beside the rail.
 * The content pane is tagged [screenTag] of the selected destination's route, so UI flows check which
 * destination is shown without matching any feature's text.
 */
@Composable
fun PocketCpShell(
    entries: List<FeatureEntry>,
    modifier: Modifier = Modifier,
    offline: Boolean = false,
    windowInsets: WindowInsets = WindowInsets.systemBars,
    navController: NavHostController = rememberNavController(),
) {
    val start = entries.firstOrNull() ?: return
    val destinations = remember(entries) { entries.map { it.destination } }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedRoute = destinations.selectedBy(backStackEntry?.destination)?.route
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
    val content: @Composable ColumnScope.() -> Unit = {
        if (offline) OfflineBanner()
        navHost(Modifier.weight(1f).fillMaxWidth().testTag(screenTag(selectedRoute ?: start.destination.route)))
    }
    when (LocalWindowLayout.current.navigationType) {
        NavigationType.BottomBar ->
            Column(modifier) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                    content = content,
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
                ShellRail(
                    destinations,
                    selectedRoute,
                    onSelect,
                    windowInsets.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(windowInsets.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)),
                    content = content,
                )
            }
    }
}

/** `screen_<route>`: the content pane's test tag while the top-level destination [route] is selected. */
internal fun screenTag(route: String): String = "screen_$route"

/**
 * The top-level destination [current] sits under: one whose route is in its hierarchy (a nested graph), or
 * whose route prefixes the current route as `<route>/...` (a sibling detail screen). `null` before the first
 * destination is shown.
 */
private fun List<TopLevelDestination>.selectedBy(current: NavDestination?): TopLevelDestination? {
    val routes =
        current
            ?.hierarchy
            .orEmpty()
            .mapNotNull { it.route }
            .toList()
    return firstOrNull { d -> routes.any { it == d.route || it.startsWith("${d.route}/") } }
}

/** Switches top-level destination: one copy of each on the stack, Home at the bottom, each tab's state saved. */
private fun NavController.navigateToTopLevel(route: String) =
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
