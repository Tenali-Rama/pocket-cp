package app.pocketcp.core.screenshot

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Robolectric's SDK for screenshot tests: SDK 35+ runtimes need JDK 21, and CI builds with JDK 17. */
const val SCREENSHOT_SDK = 34

/**
 * Base class for a module's screenshot tests: runs them on Robolectric at [SCREENSHOT_SDK].
 * Native graphics are switched on for every unit test by the `pocketcp.android.compose` convention.
 *
 * ```
 * class ContestListScreenshotTest : ScreenshotTest() {
 *     @Test fun loaded() = captureMatrix("contest_list_loaded") { ContestList(sampleContests) }
 * }
 * ```
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
abstract class ScreenshotTest
