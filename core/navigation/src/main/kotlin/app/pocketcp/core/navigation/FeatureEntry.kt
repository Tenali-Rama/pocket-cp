package app.pocketcp.core.navigation

import androidx.navigation.NavGraphBuilder
import app.pocketcp.core.model.OptionalModule

/**
 * A feature's contribution to the app shell: its top-level [destination] and the screens it adds to the nav graph.
 * A feature binds one into the `Set<FeatureEntry>` multibinding (`@IntoSet`); the shell collects the set,
 * so no feature edits the nav graph.
 */
interface FeatureEntry {
    val destination: TopLevelDestination

    /** The optional module (spec §15) this entry belongs to; `null` for core destinations, which are always shown. */
    val module: OptionalModule? get() = null

    /** Adds this feature's screens to [graph], including the one for [destination]'s route. */
    fun register(graph: NavGraphBuilder)
}

/** The entries as the shell shows them: ascending `order`, ties broken by route so the result is stable. */
fun Iterable<FeatureEntry>.inNavigationOrder(): List<FeatureEntry> =
    sortedWith(compareBy({ it.destination.order }, { it.destination.route }))

/** The entries without those whose [FeatureEntry.module] is in [disabledModules]: a disabled module leaves no trace. */
fun Iterable<FeatureEntry>.withoutDisabled(disabledModules: Set<OptionalModule>): List<FeatureEntry> =
    filter { entry -> entry.module.let { it == null || it !in disabledModules } }
