package app.pocketcp.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import app.pocketcp.core.adaptive.ProvideWindowLayout
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.navigation.FeatureEntry
import app.pocketcp.core.navigation.withoutDisabled
import kotlinx.coroutines.flow.Flow

/** The Compose root node; it exposes test tags as resource ids. */
internal const val ROOT_TAG = "shell_root"

/**
 * The app UI, live on [settings]: it draws nothing until the first emission, so the first frame is already
 * in the user's theme, then recomposes on every change.
 */
@Composable
fun PocketCpApp(
    entries: List<FeatureEntry>,
    settings: Flow<UserSettings>,
    windowInsets: WindowInsets = WindowInsets.systemBars,
    onDarkTheme: (Boolean) -> Unit = {},
) {
    val current = settings.collectAsState(initial = null).value ?: return
    PocketCpApp(entries, current, windowInsets, onDarkTheme)
}

/**
 * The whole app UI: [settings]' appearance (theme, text sizes, ligatures, density), the live window layout and
 * the [PocketCpShell] with the [entries] whose optional module is not disabled. [onDarkTheme] hears whether the
 * resolved theme is dark, so the activity can match the system bars. Test tags are exposed as resource ids
 * so Maestro can select `nav_*` items.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PocketCpApp(
    entries: List<FeatureEntry>,
    settings: UserSettings = UserSettings(),
    windowInsets: WindowInsets = WindowInsets.systemBars,
    onDarkTheme: (Boolean) -> Unit = {},
) {
    val shown = remember(entries, settings.disabledModules) { entries.withoutDisabled(settings.disabledModules) }
    PocketCpTheme(
        mode = themeModeOf(settings.theme, LocalConfiguration.current.uiMode),
        codeLigatures = settings.codeLigatures,
        density = settings.density.uiDensity,
        textScale = settings.appTextSize.scale,
        codeTextScale = settings.codeTextSize.scale,
    ) {
        val dark = PocketCpTheme.colors.isDark
        val currentOnDarkTheme by rememberUpdatedState(onDarkTheme)
        LaunchedEffect(dark) { currentOnDarkTheme(dark) }
        ProvideWindowLayout {
            PocketCpShell(
                entries = shown,
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
