package app.pocketcp.shell

import app.pocketcp.core.datastore.DensityPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.datastore.ThemePreference
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.screenshot.Device
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.ThemeWrapper
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

/** The shell themes itself from the settings, so the captures add no theme of their own. */
private val NoWrapper: ThemeWrapper = { _, content -> content() }

/**
 * `shell_<device>_<theme>`: the app root as launched, theme picked from the capture's night mode.
 * `shell_amoled_phone_dark`, `shell_compact_tablet_dark`, `shell_textLargest_phone_dark`: stored appearance settings.
 */
class ShellScreenshotTest : ScreenshotTest() {
    private val entries = shellTestEntries

    @Test
    fun shell() = captureMatrix("shell", themeWrapper = NoWrapper) { PocketCpApp(entries) }

    @Test
    fun amoled() =
        captureSingle("shell_amoled", ScreenshotConfig(dark = true), NoWrapper) {
            PocketCpApp(entries, UserSettings(theme = ThemePreference.Amoled))
        }

    @Test
    fun compactTablet() =
        captureSingle("shell_compact", ScreenshotConfig(device = Device.Tablet, dark = true), NoWrapper) {
            PocketCpApp(entries, UserSettings(density = DensityPreference.Compact))
        }

    @Test
    fun largestText() =
        captureSingle("shell_textLargest", ScreenshotConfig(dark = true), NoWrapper) {
            PocketCpApp(entries, UserSettings(appTextSize = TextSizePreference.Largest))
        }
}
