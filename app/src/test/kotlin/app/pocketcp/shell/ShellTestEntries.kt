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

/**
 * The shell tests' destinations: the five M1 ones with fixed routes, labels, icons and orders. Independent of
 * `PlaceholderModule`, so a feature replacing a placeholder changes neither the shell tests nor their goldens.
 */
val shellTestEntries: List<FeatureEntry> =
    listOf(
        PlaceholderEntry(TopLevelDestination("home", R.string.nav_home, Icons.Filled.Home, 0), R.string.placeholder_home),
        PlaceholderEntry(
            TopLevelDestination("contests", R.string.nav_contests, Icons.Filled.DateRange, 10),
            R.string.placeholder_contests,
        ),
        PlaceholderEntry(
            TopLevelDestination("problemset", R.string.nav_problemset, Icons.AutoMirrored.Filled.List, 20),
            R.string.placeholder_problemset,
        ),
        PlaceholderEntry(
            TopLevelDestination("profile", R.string.nav_profile, Icons.Filled.Person, 30),
            R.string.placeholder_profile,
        ),
        PlaceholderEntry(
            TopLevelDestination("settings", R.string.nav_settings, Icons.Filled.Settings, 40),
            R.string.placeholder_settings,
        ),
    ).inNavigationOrder()
