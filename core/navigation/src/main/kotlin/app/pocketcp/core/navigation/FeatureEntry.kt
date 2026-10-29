package app.pocketcp.core.navigation

import androidx.navigation.NavGraphBuilder

/**
 * A feature's contribution to the app shell: its top-level [destination] and the screens it adds to the nav graph.
 * A feature binds one into the `Set<FeatureEntry>` multibinding (`@IntoSet`); the shell collects the set,
 * so no feature edits the nav graph.
 */
interface FeatureEntry {
    val destination: TopLevelDestination

    /** Adds this feature's screens to [graph], including the one for [destination]'s route. */
    fun register(graph: NavGraphBuilder)
}

/** The entries as the shell shows them: ascending `order`, ties broken by route so the result is stable. */
fun Iterable<FeatureEntry>.inNavigationOrder(): List<FeatureEntry> =
    sortedWith(compareBy({ it.destination.order }, { it.destination.route }))
