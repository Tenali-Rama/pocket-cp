package app.pocketcp.core.adaptive

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.window.testing.layout.FoldingFeature as testFoldingFeature

/** [ProvideWindowLayout] on Robolectric, with fold changes published through a fake [WindowInfoTracker]. */
@RunWith(AndroidJUnit4::class)
class WindowLayoutTest {
    private val publisherRule = WindowLayoutInfoPublisherRule()
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(publisherRule).around(composeRule)

    private var layout: WindowLayout? = null

    private fun setContent() {
        composeRule.setContent {
            ProvideWindowLayout { layout = LocalWindowLayout.current }
        }
        composeRule.waitForIdle()
    }

    private fun publishFold(
        state: FoldingFeature.State,
        orientation: FoldingFeature.Orientation,
    ) {
        val fold = testFoldingFeature(activity = composeRule.activity, state = state, orientation = orientation)
        publisherRule.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(fold)))
        composeRule.waitForIdle()
    }

    @Test
    fun noFold_isFlat() {
        setContent()

        assertEquals(DevicePosture.Flat, layout?.posture)
    }

    @Test
    fun halfOpenedVerticalFold_switchesToBook_andBackToFlat() {
        setContent()

        publishFold(FoldingFeature.State.HALF_OPENED, FoldingFeature.Orientation.VERTICAL)
        assertTrue(layout?.posture is DevicePosture.Book)

        publishFold(FoldingFeature.State.FLAT, FoldingFeature.Orientation.VERTICAL)
        assertEquals(DevicePosture.Flat, layout?.posture)
    }

    @Test
    fun halfOpenedHorizontalFold_switchesToTabletop() {
        setContent()

        publishFold(FoldingFeature.State.HALF_OPENED, FoldingFeature.Orientation.HORIZONTAL)

        assertTrue(layout?.posture is DevicePosture.Tabletop)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun phoneWindow_isCompactWithBottomBar() {
        setContent()

        assertEquals(WindowWidthClass.Compact, layout?.widthClass)
        assertEquals(WindowHeightClass.Medium, layout?.heightClass)
        assertEquals(NavigationType.BottomBar, layout?.navigationType)
    }

    @Test
    @Config(qualifiers = "w1280dp-h1000dp")
    fun tabletWindow_isExpandedWithRail() {
        setContent()

        assertEquals(WindowWidthClass.Expanded, layout?.widthClass)
        assertEquals(WindowHeightClass.Expanded, layout?.heightClass)
        assertEquals(NavigationType.Rail, layout?.navigationType)
    }
}
