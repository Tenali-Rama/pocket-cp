package app.pocketcp.shell

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.pocketcp.R
import app.pocketcp.core.designsystem.component.EmptyState
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet

/** A top-level destination whose feature has not landed yet: an [EmptyState] naming the milestone that delivers it. */
class PlaceholderEntry(
    override val destination: TopLevelDestination,
    @StringRes private val message: Int,
) : FeatureEntry {
    override fun register(graph: NavGraphBuilder) {
        graph.composable(destination.route) {
            EmptyState(
                title = stringResource(destination.label),
                message = stringResource(message),
                icon = destination.icon,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** The default top-level destinations whose features have not landed yet. */
object Placeholders {
    val profile =
        placeholder(
            route = "profile",
            label = R.string.nav_profile,
            icon = Icons.Filled.Person,
            order = 30,
            message = R.string.placeholder_profile,
        )

    private fun placeholder(
        route: String,
        @StringRes label: Int,
        icon: ImageVector,
        order: Int,
        @StringRes message: Int,
    ) = PlaceholderEntry(TopLevelDestination(route, label, icon, order), message)
}

/**
 * The entries without each [PlaceholderEntry] whose route another, real entry also binds: a feature replaces its
 * placeholder just by binding a [FeatureEntry] with the same route.
 */
fun Iterable<FeatureEntry>.withoutReplacedPlaceholders(): List<FeatureEntry> {
    val realRoutes = filterNot { it is PlaceholderEntry }.mapTo(HashSet()) { it.destination.route }
    return filterNot { it is PlaceholderEntry && it.destination.route in realRoutes }
}

/**
 * Binds the placeholders. A feature that delivers one of these destinations binds an entry with the same route,
 * which drops the placeholder ([withoutReplacedPlaceholders]); it never edits this module.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaceholderModule {
    @Provides
    @ElementsIntoSet
    fun placeholders(): Set<FeatureEntry> = setOf(Placeholders.profile)
}
