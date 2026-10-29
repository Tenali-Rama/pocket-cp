package app.pocketcp.shell

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import app.pocketcp.core.adaptive.ProvideWindowLayout
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.navigation.FeatureEntry

/** The Compose root node; it exposes test tags as resource ids. */
internal const val ROOT_TAG = "shell_root"

/** Light only when the system says night mode is off; dark when it is on or unknown (dark-first, spec §5). */
fun themeModeOf(uiMode: Int): ThemeMode =
    if (uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO) ThemeMode.Light else ThemeMode.Dark

/**
 * The whole app UI: theme from the system night mode, the live window layout and the [PocketCpShell].
 * Test tags are exposed as resource ids so Maestro can select `nav_*` items.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PocketCpApp(
    entries: List<FeatureEntry>,
    windowInsets: WindowInsets = WindowInsets.systemBars,
) {
    PocketCpTheme(mode = themeModeOf(LocalConfiguration.current.uiMode)) {
        ProvideWindowLayout {
            PocketCpShell(
                entries = entries,
                windowInsets = windowInsets,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(PocketCpTheme.colors.background)
                        .semantics { testTagsAsResourceId = true }
                        .testTag(ROOT_TAG),
            )
        }
    }
}
