package app.pocketcp.shell

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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

/** The default top-level destinations, until their features replace them. */
object Placeholders {
    val home = placeholder("home", R.string.nav_home, Icons.Filled.Home, order = 0, R.string.placeholder_home)
    val contests = placeholder("contests", R.string.nav_contests, Icons.Filled.DateRange, order = 10, R.string.placeholder_contests)
    val problemset =
        placeholder("problemset", R.string.nav_problemset, Icons.AutoMirrored.Filled.List, order = 20, R.string.placeholder_problemset)
    val profile = placeholder("profile", R.string.nav_profile, Icons.Filled.Person, order = 30, R.string.placeholder_profile)
    val settings = placeholder("settings", R.string.nav_settings, Icons.Filled.Settings, order = 40, R.string.placeholder_settings)

    private fun placeholder(
        route: String,
        @StringRes label: Int,
        icon: ImageVector,
        order: Int,
        @StringRes message: Int,
    ) = PlaceholderEntry(TopLevelDestination(route, label, icon, order), message)
}

/** Binds the placeholders. A feature that delivers one of these destinations deletes its line here. */
@Module
@InstallIn(SingletonComponent::class)
object PlaceholderModule {
    @Provides
    @ElementsIntoSet
    fun placeholders(): Set<FeatureEntry> =
        setOf(
            Placeholders.home,
            Placeholders.contests,
            Placeholders.problemset,
            Placeholders.profile,
            Placeholders.settings,
        )
}
