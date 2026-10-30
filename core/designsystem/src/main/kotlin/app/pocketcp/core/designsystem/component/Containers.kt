package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme

/**
 * A grouped block of content on `colors.surface` with a hairline `colors.outline` border, so it stays visible
 * on AMOLED where surface and background are both black. With [onClick] the whole card is one click target.
 * The card adds no padding: rows pad themselves; pad other content `spacing.lg` horizontally and
 * `spacing.cardVertical` vertically, so it follows `PocketCpTheme(density = …)`.
 */
@Composable
fun PcpCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PocketCpTheme.colors
    val shape = PocketCpTheme.shapes.large
    val cardColors =
        CardDefaults.cardColors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            disabledContainerColor = colors.surface,
            disabledContentColor = colors.onSurfaceMuted,
        )
    val border = BorderStroke(Dp.Hairline, colors.outline)
    if (onClick == null) {
        Card(modifier = modifier, shape = shape, colors = cardColors, border = border, content = content)
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = cardColors,
            border = border,
            content = content,
        )
    }
}

/**
 * One row of a list: a [headline], optional [supporting] line, and [leading] / [trailing] slots.
 * Transparent, so it sits on a [PcpCard] or the screen background. With [onClick] the row is clickable;
 * a disabled row shows its headline in `colors.onSurfaceMuted` and ignores clicks.
 * Its vertical padding and minimum height follow `PocketCpTheme(density = …)`.
 */
@Composable
fun PcpListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = PocketCpTheme.colors
    val leadingColor = if (enabled) colors.onSurface else colors.onSurfaceMuted
    PcpRow(
        headline = headline,
        supporting = supporting,
        enabled = enabled,
        modifier = if (onClick == null) modifier else modifier.clickable(enabled = enabled, onClick = onClick),
        leading =
            leading?.let {
                { CompositionLocalProvider(LocalContentColor provides leadingColor, content = it) }
            },
        trailing =
            trailing?.let {
                { CompositionLocalProvider(LocalContentColor provides colors.onSurfaceMuted, content = it) }
            },
    )
}

/**
 * The layout shared by list, switch and choice rows: [leading], then [headline] over [supporting], then [trailing],
 * centred vertically, at least `spacing.listItemHeight` (or `listItemTwoLineHeight`) tall. One merged
 * accessibility node; [modifier] carries the row's click, toggle or selection behaviour.
 */
@Composable
internal fun PcpRow(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = PocketCpTheme.colors
    val typography = PocketCpTheme.typography
    val spacing = PocketCpTheme.spacing
    Row(
        modifier =
            Modifier
                .semantics(mergeDescendants = true) {}
                .then(modifier)
                .heightIn(min = if (supporting == null) spacing.listItemHeight else spacing.listItemTwoLineHeight)
                .padding(horizontal = spacing.lg, vertical = spacing.listItemVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { Box(Modifier.padding(end = spacing.lg)) { it() } }
        Column(Modifier.weight(1f)) {
            Text(
                text = headline,
                style = typography.body,
                color = if (enabled) colors.onSurface else colors.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.let { Text(text = it, style = typography.bodySmall, color = colors.onSurfaceMuted) }
        }
        trailing?.let { Box(Modifier.padding(start = spacing.lg)) { it() } }
    }
}
