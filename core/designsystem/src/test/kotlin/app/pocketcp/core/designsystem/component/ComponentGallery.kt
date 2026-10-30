package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.Rank
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.designsystem.UiDensity
import app.pocketcp.core.screenshot.ThemeWrapper

/** Windows at least this wide (Material 3 medium width) lay the galleries out side by side. */
private val WideMinWidth = 600.dp

/** [PocketCpTheme] dark or light, for `captureMatrix`. */
internal val PocketCpThemeWrapper: ThemeWrapper = themeWrapper()

/** [PocketCpTheme] dark or light with the given [density] and [textScale]. */
internal fun themeWrapper(
    density: UiDensity = UiDensity.Comfortable,
    textScale: Float = 1f,
): ThemeWrapper =
    { dark, content ->
        PocketCpTheme(
            mode = if (dark) ThemeMode.Dark else ThemeMode.Light,
            density = density,
            textScale = textScale,
            content = content,
        )
    }

/** Every component, enabled and disabled: controls on the left, content on the right on wide windows. */
@Composable
internal fun ComponentGallery() {
    val spacing = PocketCpTheme.spacing
    Column(Modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
        PcpTopAppBar(
            title = "Components",
            onNavigateBack = {},
            actions = { PcpIconButton(Icons.Filled.Search, contentDescription = "Search", onClick = {}) },
        )
        BoxWithConstraints(Modifier.padding(horizontal = spacing.lg)) {
            if (maxWidth >= WideMinWidth) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xl)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.md)) { Controls() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.md)) { Content() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Controls()
                    Content()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controls() {
    val spacing = PocketCpTheme.spacing
    listOf(true, false).forEach { enabled ->
        Text(
            text = if (enabled) "Enabled" else "Disabled",
            style = PocketCpTheme.typography.label,
            color = PocketCpTheme.colors.onSurfaceMuted,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            PcpPrimaryButton("Primary", onClick = {}, enabled = enabled)
            PcpSecondaryButton("Secondary", onClick = {}, enabled = enabled)
            PcpTextButton("Text", onClick = {}, enabled = enabled)
            PcpIconButton(Icons.Filled.Refresh, contentDescription = "Refresh", onClick = {}, enabled = enabled)
            PcpFilterChip("Div. 2", selected = true, onClick = {}, enabled = enabled)
            PcpFilterChip("Div. 3", selected = false, onClick = {}, enabled = enabled)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Content() {
    val spacing = PocketCpTheme.spacing
    PcpCard(Modifier.fillMaxWidth()) {
        PcpListItem(
            headline = "Codeforces Round (Div. 2)",
            supporting = "Starts in 2 days",
            trailing = { Icon(Icons.Outlined.Star, contentDescription = null) },
            onClick = {},
        )
        PcpListItem(
            headline = "Educational Round (disabled)",
            supporting = "Registration closed",
            trailing = { Icon(Icons.Outlined.Star, contentDescription = null) },
            onClick = {},
            enabled = false,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        listOf(true, false).forEach { enabled ->
            PcpCard(Modifier.weight(1f), onClick = {}, enabled = enabled) {
                Text(
                    text = if (enabled) "Clickable card" else "Disabled card",
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.cardVertical),
                    style = PocketCpTheme.typography.body,
                )
            }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        RankedHandle("legend", Rank.LegendaryGrandmaster)
        RankedHandle("expert", Rank.Expert)
        RankedHandle("newcomer", rating = null)
    }
}

/** One handle per rank, newbie to legendary grandmaster, each with its lowest rating. */
@Composable
internal fun RankedHandleSheet() {
    val spacing = PocketCpTheme.spacing
    Column(
        Modifier.fillMaxSize().background(PocketCpTheme.colors.background).padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Rank.entries.filter { it != Rank.Unrated }.forEach { rank ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankedHandle(rank.name, rank, Modifier.weight(1f), style = PocketCpTheme.typography.title)
                Text(
                    text = "${rank.minRating?.coerceAtLeast(0)}+",
                    style = PocketCpTheme.typography.bodySmall,
                    color = PocketCpTheme.colors.onSurfaceMuted,
                )
            }
        }
    }
}

/** Empty, error and loading states: side by side on wide windows, stacked on a phone. */
@Composable
internal fun StatesGallery() {
    val spacing = PocketCpTheme.spacing
    val states: List<@Composable () -> Unit> =
        listOf(
            { EmptyState(title = "No contests", message = "Nothing scheduled right now.", modifier = Modifier.fillMaxSize()) },
            { ErrorState(message = "Codeforces is not responding.", onRetry = {}, modifier = Modifier.fillMaxSize()) },
            { LoadingPlaceholder(Modifier.fillMaxSize(), rows = 3) },
        )
    BoxWithConstraints(Modifier.fillMaxSize().background(PocketCpTheme.colors.background).padding(spacing.lg)) {
        if (maxWidth >= WideMinWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                states.forEach { PcpCard(Modifier.weight(1f).fillMaxHeight()) { it() } }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                states.forEach { PcpCard(Modifier.weight(1f).fillMaxWidth()) { it() } }
            }
        }
    }
}
