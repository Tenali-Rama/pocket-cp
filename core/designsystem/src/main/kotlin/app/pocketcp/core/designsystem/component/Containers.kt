package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme

/**
 * A grouped block of content on `colors.surface` with a hairline `colors.outline` border, so it stays visible
 * on AMOLED where surface and background are both black. With [onClick] the whole card is one click target.
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
    val typography = PocketCpTheme.typography
    ListItem(
        headlineContent = {
            Text(
                text = headline,
                style = typography.body,
                color = if (enabled) colors.onSurface else colors.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = if (onClick == null) modifier else modifier.clickable(enabled = enabled, onClick = onClick),
        supportingContent =
            supporting?.let {
                { Text(text = it, style = typography.bodySmall, color = colors.onSurfaceMuted) }
            },
        leadingContent = leading,
        trailingContent = trailing,
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                leadingIconColor = if (enabled) colors.onSurface else colors.onSurfaceMuted,
                trailingIconColor = colors.onSurfaceMuted,
            ),
    )
}
