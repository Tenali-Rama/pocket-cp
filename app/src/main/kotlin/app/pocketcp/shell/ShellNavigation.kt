package app.pocketcp.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.navigation.TopLevelDestination

/** The bottom bar including its inset padding. */
internal const val BOTTOM_BAR_TAG = "shell_bottom_bar"

/** The bottom bar's items row, inside its inset padding. */
internal const val BOTTOM_BAR_ITEMS_TAG = "shell_bottom_bar_items"

/** The navigation rail including its inset padding. */
internal const val RAIL_TAG = "shell_rail"

/** The rail's items column, inside its inset padding. */
internal const val RAIL_ITEMS_TAG = "shell_rail_items"

/** No insets: the shell pads the bar and rail itself, so the tags measure that padding (`ShellInsetsTest`). */
private val NoInsets = WindowInsets(0, 0, 0, 0)

/** Phone chrome: a hairline, then the items; [insets] (bottom + horizontal bars) padded inside the bar colour. */
@Composable
internal fun ShellBottomBar(
    destinations: List<TopLevelDestination>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    insets: WindowInsets,
) {
    val colors = PocketCpTheme.colors
    Column(
        Modifier
            .testTag(BOTTOM_BAR_TAG)
            .background(colors.surface)
            .windowInsetsPadding(insets),
    ) {
        HorizontalDivider(color = colors.outline)
        NavigationBar(
            modifier = Modifier.testTag(BOTTOM_BAR_ITEMS_TAG),
            containerColor = colors.surface,
            windowInsets = NoInsets,
        ) {
            destinations.forEach { destination ->
                val label = stringResource(destination.label)
                NavigationBarItem(
                    selected = destination.route == selectedRoute,
                    onClick = { onSelect(destination.route) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { NavLabel(label) },
                    modifier = Modifier.navItem(destination, label),
                )
            }
        }
    }
}

/** Large-window chrome: the items, then a hairline; [insets] (start + vertical bars) padded inside the rail. */
@Composable
internal fun ShellRail(
    destinations: List<TopLevelDestination>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    insets: WindowInsets,
) {
    val colors = PocketCpTheme.colors
    Row(
        Modifier
            .testTag(RAIL_TAG)
            .fillMaxHeight()
            .background(colors.surface)
            .windowInsetsPadding(insets),
    ) {
        NavigationRail(
            modifier = Modifier.testTag(RAIL_ITEMS_TAG),
            containerColor = colors.surface,
            windowInsets = NoInsets,
        ) {
            destinations.forEach { destination ->
                val label = stringResource(destination.label)
                NavigationRailItem(
                    selected = destination.route == selectedRoute,
                    onClick = { onSelect(destination.route) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { NavLabel(label) },
                    modifier = Modifier.navItem(destination, label),
                )
            }
        }
        VerticalDivider(color = colors.outline)
    }
}

/** The smallest a nav label shrinks to, as a fraction of the theme's label size. */
private const val MIN_LABEL_SCALE = 0.7f

/**
 * A nav item's label: one line at the theme's label size, shrunk step by step (down to [MIN_LABEL_SCALE])
 * rather than wrapped or clipped when a large text size makes it wider than its item.
 */
@Composable
private fun NavLabel(text: String) {
    val style = LocalTextStyle.current.copy(color = LocalContentColor.current)
    BasicText(
        text = text,
        style = style,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = style.fontSize * MIN_LABEL_SCALE, maxFontSize = style.fontSize),
    )
}

/** `nav_<route>` for tests and Maestro, and the label as the item's content description. */
private fun Modifier.navItem(
    destination: TopLevelDestination,
    label: String,
): Modifier = testTag(destination.testTag).semantics { contentDescription = label }
