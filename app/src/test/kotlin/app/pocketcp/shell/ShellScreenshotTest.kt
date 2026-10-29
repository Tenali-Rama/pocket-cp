package app.pocketcp.shell

import app.pocketcp.core.navigation.inNavigationOrder
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureMatrix
import org.junit.Test

/** `shell_<device>_<theme>`: the app root as launched, theme picked from the capture's night mode. */
class ShellScreenshotTest : ScreenshotTest() {
    private val entries = PlaceholderModule.placeholders().inNavigationOrder()

    @Test
    fun shell() = captureMatrix("shell", themeWrapper = { _, content -> content() }) { PocketCpApp(entries) }
}
