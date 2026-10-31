package app.pocketcp.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import app.pocketcp.R
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.TopLevelDestination
import app.pocketcp.core.navigation.inNavigationOrder
import app.pocketcp.feature.contests.R as ContestsR
import app.pocketcp.feature.home.R as HomeR
import app.pocketcp.feature.problemset.R as ProblemsetR
import app.pocketcp.feature.settings.R as SettingsR

/**
 * The shell tests' destinations: the five top-level ones with fixed routes, labels, icons and orders, each an
 * `EmptyState` titled by its label. Independent of the bound features and `PlaceholderModule`, so a feature
 * changing its screen changes neither the shell tests nor their goldens.
 */
val shellTestEntries: List<FeatureEntry> =
    listOf(
        PlaceholderEntry(
            TopLevelDestination("home", HomeR.string.home_label, Icons.Filled.Home, 0),
            HomeR.string.home_label,
        ),
        PlaceholderEntry(
            TopLevelDestination("contests", ContestsR.string.contests_label, Icons.Filled.DateRange, 10),
            ContestsR.string.contests_label,
        ),
        PlaceholderEntry(
            TopLevelDestination("problemset", ProblemsetR.string.problemset_label, Icons.AutoMirrored.Filled.List, 20),
            ProblemsetR.string.problemset_label,
        ),
        PlaceholderEntry(
            TopLevelDestination("profile", R.string.nav_profile, Icons.Filled.Person, 30),
            R.string.placeholder_profile,
        ),
        PlaceholderEntry(
            TopLevelDestination("settings", SettingsR.string.settings_label, Icons.Filled.Settings, 40),
            SettingsR.string.settings_label,
        ),
    ).inNavigationOrder()
