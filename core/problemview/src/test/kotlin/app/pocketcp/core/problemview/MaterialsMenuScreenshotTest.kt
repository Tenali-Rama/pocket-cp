@file:OptIn(ExperimentalRoborazziApi::class)

package app.pocketcp.core.problemview

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.screenshot.Device
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import app.pocketcp.core.screenshot.ScreenshotConfig
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.github.takahirom.roborazzi.roborazziSystemPropertyOutputDirectory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * The contest materials menu open over the statement, on every device in dark and light. The menu is a popup window,
 * which `captureMatrix` (the activity's content view) leaves out, so each test captures the whole screen in a window
 * of its device's size instead (as [ReadingMenuScreenshotTest] does).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
class MaterialsMenuScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = PHONE)
    fun phoneDark() = capture(Device.Phone, dark = true)

    @Test
    @Config(qualifiers = PHONE)
    fun phoneLight() = capture(Device.Phone, dark = false)

    @Test
    @Config(qualifiers = TABLET)
    fun tabletDark() = capture(Device.Tablet, dark = true)

    @Test
    @Config(qualifiers = TABLET)
    fun tabletLight() = capture(Device.Tablet, dark = false)

    @Test
    @Config(qualifiers = FOLDABLE)
    fun foldableDark() = capture(Device.Foldable, dark = true)

    @Test
    @Config(qualifiers = FOLDABLE)
    fun foldableLight() = capture(Device.Foldable, dark = false)

    @Test
    @Config(qualifiers = DESKTOP)
    fun desktopDark() = capture(Device.Desktop, dark = true)

    @Test
    @Config(qualifiers = DESKTOP)
    fun desktopLight() = capture(Device.Desktop, dark = false)

    private fun capture(
        device: Device,
        dark: Boolean,
    ) {
        val state = ProblemUiState(Paco, ProblemStatus.Loaded, recordedStatement())
        rule.setContent {
            PocketCpThemeWrapper(dark) {
                ScreenshotFrame {
                    ProblemScreen(state, RecordingActions(), onBack = {})
                }
            }
        }
        rule.onNodeWithTag(MATERIALS_MENU_TAG).performClick()
        rule.waitForIdle()

        val file = ScreenshotConfig(device, dark).fileName("problemView_materials")
        captureScreenRoboImage(File(roborazziSystemPropertyOutputDirectory(), file))
    }

    private companion object {
        // Each device's window at density 1, so the golden's pixels are its size in dp (core/screenshot's Device).
        const val PHONE = "w411dp-h914dp-mdpi"
        const val TABLET = "w1280dp-h800dp-land-mdpi"
        const val FOLDABLE = "w673dp-h841dp-mdpi"
        const val DESKTOP = "w1440dp-h900dp-land-mdpi"
    }
}
