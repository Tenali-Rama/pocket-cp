package app.pocketcp.core.designsystem.component

import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

/** `components_*`, `components_font150_phone_dark`, `rankedHandle_phone_*` and `states_*`. */
class ComponentsScreenshotTest : ScreenshotTest() {
    @Test
    fun components() = captureMatrix("components", themeWrapper = PocketCpThemeWrapper) { ComponentGallery() }

    @Test
    fun componentsLargeFont() =
        captureSingle("components_font150", ScreenshotConfig(fontScale = 1.5f), PocketCpThemeWrapper) {
            ComponentGallery()
        }

    @Test
    fun rankedHandleDark() = captureSingle("rankedHandle", ScreenshotConfig(dark = true), PocketCpThemeWrapper) { RankedHandleSheet() }

    @Test
    fun rankedHandleLight() = captureSingle("rankedHandle", ScreenshotConfig(dark = false), PocketCpThemeWrapper) { RankedHandleSheet() }

    @Test
    fun states() = captureMatrix("states", themeWrapper = PocketCpThemeWrapper) { StatesGallery() }
}
