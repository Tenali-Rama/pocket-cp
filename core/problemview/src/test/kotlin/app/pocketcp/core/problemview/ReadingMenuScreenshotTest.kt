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
 * The reading menu open over the statement. The menu is a popup window, which `captureSingle` (the activity's
 * content view) leaves out, so this captures the whole screen in a phone-sized window instead.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "w411dp-h914dp-mdpi")
class ReadingMenuScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun readingMenu() {
        val state = ProblemUiState(Paco, ProblemStatus.Loaded, recordedStatement())
        rule.setContent {
            PocketCpThemeWrapper(true) {
                ScreenshotFrame {
                    ProblemScreen(state, RecordingActions(), onBack = {})
                }
            }
        }
        rule.onNodeWithTag(READING_MENU_TAG).performClick()
        rule.waitForIdle()

        val file = ScreenshotConfig(Device.Phone, dark = true).fileName("problemView_readingMenu")
        captureScreenRoboImage(File(roborazziSystemPropertyOutputDirectory(), file))
    }
}
