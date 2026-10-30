package app.pocketcp.core.ui

import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

/** `browseRows_<device>_<theme>`, `browseRows_hiddenSpoilers_phone_dark` and `browseRows_font150_phone_dark`. */
class BrowseRowsScreenshotTest : ScreenshotTest() {
    @Test
    fun browseRows() = captureMatrix("browseRows", themeWrapper = browseThemeWrapper()) { BrowseRowsGallery() }

    @Test
    fun browseRowsHiddenSpoilers() =
        captureSingle("browseRows_hiddenSpoilers", ScreenshotConfig(dark = true), browseThemeWrapper()) {
            BrowseRowsGallery(hideTags = true, hideRating = true)
        }

    @Test
    fun browseRowsLargeFont() =
        captureSingle("browseRows_font150", ScreenshotConfig(fontScale = 1.5f), browseThemeWrapper()) {
            BrowseRowsGallery()
        }
}
