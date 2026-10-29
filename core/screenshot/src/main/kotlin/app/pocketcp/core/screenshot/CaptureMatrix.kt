@file:OptIn(ExperimentalRoborazziApi::class)

package app.pocketcp.core.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.RoborazziComposeSetupOption
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.fontScale
import com.github.takahirom.roborazzi.roborazziSystemPropertyOutputDirectory
import com.github.takahirom.roborazzi.size
import com.github.takahirom.roborazzi.uiMode
import java.io.File

/** Wraps captured content in a theme; `dark` picks the dark or the light variant. */
typealias ThemeWrapper = @Composable (dark: Boolean, content: @Composable () -> Unit) -> Unit

/** The default [ThemeWrapper]: plain Material 3 dark/light colours on a full-window surface. */
val MaterialThemeWrapper: ThemeWrapper = { dark, content ->
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), content = content)
    }
}

/**
 * Captures [content] on every [Device] in dark and light: 8 files `<name>_<device>_<theme>.png`
 * in the calling module's `src/test/screenshots/`.
 * Under Roborazzi verify every configuration is still rendered when one fails, so CI collects all of them;
 * the first failure is then thrown with the others suppressed.
 */
fun captureMatrix(
    name: String,
    themeWrapper: ThemeWrapper = MaterialThemeWrapper,
    content: @Composable () -> Unit,
) {
    val captures =
        Device.entries.flatMap { device ->
            listOf(true, false).map { dark ->
                { captureSingle(name, ScreenshotConfig(device, dark), themeWrapper, content) }
            }
        }
    runAllCaptures(captures)
}

/**
 * Captures [content] in one [config], to `<name>_<device>_<theme>.png`.
 * For extra shots such as AMOLED (pass its [themeWrapper]) or 1.5x font (set `fontScale`); put the variant in [name].
 */
fun captureSingle(
    name: String,
    config: ScreenshotConfig = ScreenshotConfig(),
    themeWrapper: ThemeWrapper = MaterialThemeWrapper,
    content: @Composable () -> Unit,
) {
    val (device, dark, fontScale) = config
    val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    val options =
        RoborazziComposeOptions {
            addOption(BaselineDensity)
            size(widthDp = device.widthDp, heightDp = device.heightDp)
            uiMode(Configuration.UI_MODE_TYPE_NORMAL or night)
            // Always set: Robolectric keeps the font scale for later captures in the same test.
            fontScale(fontScale)
        }
    captureRoboImage(
        file = File(roborazziSystemPropertyOutputDirectory(), config.fileName(name)),
        roborazziComposeOptions = options,
    ) {
        themeWrapper(dark, content)
    }
}

/** Runs every capture; rethrows the first [AssertionError] (a Roborazzi diff) after all ran, the rest suppressed. */
internal fun runAllCaptures(captures: List<() -> Unit>) {
    val failures =
        captures.mapNotNull { capture ->
            try {
                capture()
                null
            } catch (e: AssertionError) {
                e
            }
        }
    val first = failures.firstOrNull() ?: return
    failures.drop(1).forEach(first::addSuppressed)
    throw first
}

/** Density 1 (mdpi), so a golden's pixel size equals the device size in dp, whatever the test's own config. */
private object BaselineDensity : RoborazziComposeSetupOption {
    override fun configure(configBuilder: RoborazziComposeSetupOption.ConfigBuilder) {
        configBuilder.addRobolectricQualifier("mdpi")
    }
}
