package app.pocketcp.core.designsystem

import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

/** The token sheet in every theme: `themeTokens_<device>_<dark|light>` plus `themeTokens_amoled_phone_dark`. */
class ThemeTokensScreenshotTest : ScreenshotTest() {
    @Test
    fun themeTokens() =
        captureMatrix("themeTokens", themeWrapper = { dark, content ->
            PocketCpTheme(mode = if (dark) ThemeMode.Dark else ThemeMode.Light, content = content)
        }) {
            ThemeTokens(mode = if (PocketCpTheme.colors.isDark) ThemeMode.Dark else ThemeMode.Light)
        }

    @Test
    fun themeTokensAmoled() =
        captureSingle("themeTokens_amoled", ScreenshotConfig(dark = true), themeWrapper = { _, content ->
            PocketCpTheme(mode = ThemeMode.Amoled, content = content)
        }) {
            ThemeTokens(mode = ThemeMode.Amoled)
        }
}
