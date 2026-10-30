package app.pocketcp.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.component.PcpTextButton
import app.pocketcp.core.model.JudgeError

/**
 * Shown above a list that still has cached data when its refresh failed (spec §2: cache first, never a blank
 * screen): one message per [JudgeError] case, the judge's own message for [JudgeError.Rejected], and a Retry
 * button that calls [onRetry]. Announced by TalkBack when it appears.
 */
@Composable
fun RefreshErrorBanner(
    error: JudgeError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PocketCpTheme.colors
    val spacing = PocketCpTheme.spacing
    val shape = PocketCpTheme.shapes.medium
    Row(
        modifier =
            modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .background(colors.surfaceRaised, shape)
                .border(BorderStroke(Dp.Hairline, colors.outline), shape)
                .heightIn(min = spacing.touchTarget)
                .padding(start = spacing.lg, end = spacing.xs, top = spacing.xs, bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = colors.error,
            modifier = Modifier.size(spacing.xl),
        )
        Text(
            text = refreshErrorMessage(error),
            style = PocketCpTheme.typography.body,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        PcpTextButton(text = stringResource(R.string.ui_retry), onClick = onRetry)
    }
}

/** The user-facing message for [error]. */
@Composable
private fun refreshErrorMessage(error: JudgeError): String =
    when (error) {
        is JudgeError.Offline -> stringResource(R.string.ui_refresh_error_offline)
        is JudgeError.RateLimited -> stringResource(R.string.ui_refresh_error_rate_limited)
        is JudgeError.Unavailable -> stringResource(R.string.ui_refresh_error_unavailable)
        is JudgeError.Malformed -> stringResource(R.string.ui_refresh_error_malformed)
        is JudgeError.Rejected -> error.message.orEmpty()
    }
