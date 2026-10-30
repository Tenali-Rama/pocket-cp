package app.pocketcp.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.pocketcp.R
import app.pocketcp.core.designsystem.PocketCpTheme

/** The offline banner's test tag. */
internal const val OFFLINE_BANNER_TAG = "offline_banner"

/**
 * The slim strip across the top of the content while the device is offline (spec §14). A polite live region, so
 * TalkBack announces it once when it appears.
 */
@Composable
internal fun OfflineBanner(modifier: Modifier = Modifier) {
    val spacing = PocketCpTheme.spacing
    Text(
        text = stringResource(R.string.shell_offline),
        modifier =
            modifier
                .testTag(OFFLINE_BANNER_TAG)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .fillMaxWidth()
                .background(PocketCpTheme.colors.surfaceRaised)
                .padding(horizontal = spacing.lg, vertical = spacing.xs),
        color = PocketCpTheme.colors.onSurface,
        style = PocketCpTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}
