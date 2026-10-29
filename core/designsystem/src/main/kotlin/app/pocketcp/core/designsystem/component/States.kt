package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.R

/** Skeleton line widths, as fractions of the row: a long headline over a shorter supporting line. */
private const val HEADLINE_FRACTION = 0.7f
private const val SUPPORTING_FRACTION = 0.4f

/** Rows a [LoadingPlaceholder] shows by default: about a phone screen of list items. */
private const val DEFAULT_SKELETON_ROWS = 5

/**
 * Shown when a list or screen has nothing to show: an [icon], a [title], an optional [message] and an
 * optional [action] (e.g. a [PcpSecondaryButton]). Centred in the space it is given.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Outlined.Info,
    action: (@Composable () -> Unit)? = null,
) {
    StateLayout(modifier = modifier, icon = icon, iconTint = PocketCpTheme.colors.onSurfaceMuted, title = title) {
        if (message != null) StateMessage(message)
        action?.invoke()
    }
}

/**
 * Shown when loading failed and there is no cached data: the [message] and a Retry button that calls [onRetry].
 * Announced by TalkBack when it appears.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StateLayout(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        icon = Icons.Outlined.Warning,
        iconTint = PocketCpTheme.colors.error,
        title = null,
    ) {
        StateMessage(message)
        PcpSecondaryButton(text = stringResource(R.string.designsystem_retry), onClick = onRetry)
    }
}

/**
 * Skeleton rows standing in for a list while it loads for the first time (never a blank spinner).
 * Static, no shimmer (spec §5: no decorative animation). TalkBack reads it as one "Loading" node.
 */
@Composable
fun LoadingPlaceholder(
    modifier: Modifier = Modifier,
    rows: Int = DEFAULT_SKELETON_ROWS,
) {
    val spacing = PocketCpTheme.spacing
    val description = stringResource(R.string.designsystem_loading)
    Column(
        modifier =
            modifier
                .clearAndSetSemantics { contentDescription = description }
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        repeat(rows) { SkeletonRow() }
    }
}

@Composable
private fun SkeletonRow() {
    val spacing = PocketCpTheme.spacing
    val bone = PocketCpTheme.colors.outline
    val shape = PocketCpTheme.shapes.small
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
        Box(Modifier.size(spacing.xxl).background(bone, CircleShape))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Box(Modifier.fillMaxWidth(HEADLINE_FRACTION).height(spacing.md).background(bone, shape))
            Box(Modifier.fillMaxWidth(SUPPORTING_FRACTION).height(spacing.sm).background(bone, shape))
        }
    }
}

@Composable
private fun StateLayout(
    modifier: Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String?,
    content: @Composable () -> Unit,
) {
    val spacing = PocketCpTheme.spacing
    Column(
        modifier = modifier.padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(spacing.xxl), tint = iconTint)
        if (title != null) {
            Text(
                text = title,
                style = PocketCpTheme.typography.title,
                color = PocketCpTheme.colors.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        content()
    }
}

@Composable
private fun StateMessage(message: String) {
    Text(
        text = message,
        style = PocketCpTheme.typography.body,
        color = PocketCpTheme.colors.onSurfaceMuted,
        textAlign = TextAlign.Center,
    )
}
