package app.pocketcp.core.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One item of the app's bottom bar / navigation rail.
 * [route] is its nav route and names its [testTag]; [label] is its text and content description.
 * The shell shows items in ascending [order].
 */
@Immutable
data class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    val order: Int,
) {
    /** `nav_<route>`: the item's test tag, which Maestro sees as a resource id. */
    val testTag: String get() = "nav_$route"
}
