package app.pocketcp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.designsystem.PocketCpTheme

/**
 * The app's name and version, the unofficial-client disclaimer (spec §17) and the licence. The name and version are
 * left out while [app] is `null` (still being read from the package manager).
 */
@Composable
internal fun AboutSettings(
    app: AppInfo?,
    modifier: Modifier = Modifier,
) {
    val spacing = PocketCpTheme.spacing
    val typography = PocketCpTheme.typography
    val colors = PocketCpTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (app != null) {
            AppHeader(app)
        }
        Text(
            text = stringResource(R.string.settings_about_disclaimer),
            style = typography.body,
            color = colors.onBackground,
            modifier = Modifier.testTag("settings_about_disclaimer"),
        )
        Text(
            text = stringResource(R.string.settings_about_licence),
            style = typography.bodySmall,
            color = colors.onSurfaceMuted,
            modifier = Modifier.testTag("settings_about_licence"),
        )
    }
}

/** The app's name as a heading over its version ("Version unknown" when the package has no `versionName`). */
@Composable
private fun AppHeader(app: AppInfo) {
    val typography = PocketCpTheme.typography
    val colors = PocketCpTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.xs)) {
        Text(
            text = app.name,
            style = typography.title,
            color = colors.onBackground,
            modifier = Modifier.testTag("settings_about_name").semantics { heading() },
        )
        Text(
            text =
                app.versionName?.let { stringResource(R.string.settings_about_version, it) }
                    ?: stringResource(R.string.settings_about_version_unknown),
            style = typography.bodySmall,
            color = colors.onSurfaceMuted,
            modifier = Modifier.testTag("settings_about_version"),
        )
    }
}
