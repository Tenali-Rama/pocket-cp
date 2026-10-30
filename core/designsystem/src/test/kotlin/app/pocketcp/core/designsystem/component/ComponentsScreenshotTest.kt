package app.pocketcp.core.designsystem.component

import app.pocketcp.core.designsystem.UiDensity
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

/** The largest app text size the Settings screen offers. */
private const val LARGEST_TEXT_SCALE = 1.3f

/**
 * `components_*`, `components_compact_*`, `components_font150_phone_dark`, `components_textLargest_phone_dark`,
 * `controls_*`, `rankedHandle_phone_*` and `states_*`.
 */
class ComponentsScreenshotTest : ScreenshotTest() {
    @Test
    fun components() = captureMatrix("components", themeWrapper = PocketCpThemeWrapper) { ComponentGallery() }

    @Test
    fun componentsLargeFont() =
        captureSingle("components_font150", ScreenshotConfig(fontScale = 1.5f), PocketCpThemeWrapper) {
            ComponentGallery()
        }

    @Test
    fun componentsCompact() =
        captureMatrix("components_compact", themeWrapper = themeWrapper(density = UiDensity.Compact)) { ComponentGallery() }

    @Test
    fun componentsLargestText() =
        captureSingle("components_textLargest", ScreenshotConfig(dark = true), themeWrapper(textScale = LARGEST_TEXT_SCALE)) {
            ComponentGallery()
        }

    @Test
    fun controls() = captureMatrix("controls", themeWrapper = PocketCpThemeWrapper) { ControlsGallery() }

    @Test
    fun rankedHandleDark() = captureSingle("rankedHandle", ScreenshotConfig(dark = true), PocketCpThemeWrapper) { RankedHandleSheet() }

    @Test
    fun rankedHandleLight() = captureSingle("rankedHandle", ScreenshotConfig(dark = false), PocketCpThemeWrapper) { RankedHandleSheet() }

    @Test
    fun states() = captureMatrix("states", themeWrapper = PocketCpThemeWrapper) { StatesGallery() }
}
