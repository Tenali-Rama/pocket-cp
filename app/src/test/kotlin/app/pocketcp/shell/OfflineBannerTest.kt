package app.pocketcp.shell

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.datastore.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The shell's offline banner: shown only while offline, above the content, announced politely (spec §14). */
@RunWith(AndroidJUnit4::class)
class OfflineBannerTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val entries = shellTestEntries
    private val fakeInsets = WindowInsets(top = 24.dp, left = 16.dp, bottom = 48.dp)
    private val liveRegion = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

    @Test
    fun online_noBanner() {
        rule.setContent { PocketCpApp(entries, online = true) }

        rule.onNodeWithTag(OFFLINE_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    fun offline_bannerIsShownAsPoliteLiveRegion() {
        rule.setContent { PocketCpApp(entries, online = false) }

        rule
            .onNodeWithTag(OFFLINE_BANNER_TAG)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Offline · showing saved data").assertIsDisplayed()
    }

    @Test
    fun eachChange_addsOrRemovesExactlyOneLiveRegion() {
        var online by mutableStateOf(true)
        rule.setContent { PocketCpApp(entries, online = online) }
        rule.onAllNodes(liveRegion).assertCountEquals(0)

        online = false
        rule.onAllNodes(liveRegion).assertCountEquals(1)

        online = true
        rule.onAllNodes(liveRegion).assertCountEquals(0)

        online = false
        rule.onAllNodes(liveRegion).assertCountEquals(1)
    }

    @Test
    fun repeatedOfflineEmissions_keepOneBanner() {
        val settings = flowOf(UserSettings())
        val isOnline = MutableSharedFlow<Boolean>(replay = 1)
        rule.setContent { PocketCpApp(entries, settings, isOnline) }
        rule.onNodeWithTag(OFFLINE_BANNER_TAG).assertDoesNotExist()

        isOnline.tryEmit(false)
        rule.waitForIdle()
        isOnline.tryEmit(false)
        rule.waitForIdle()

        rule.onAllNodesWithTag(OFFLINE_BANNER_TAG).assertCountEquals(1)
        rule.onAllNodes(liveRegion).assertCountEquals(1)

        isOnline.tryEmit(true)
        rule.waitForIdle()
        rule.onNodeWithTag(OFFLINE_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun phone_bannerIsUnderStatusBarAndAboveBottomBar() {
        rule.setContent { PocketCpApp(entries, online = false, windowInsets = fakeInsets) }

        val banner = rule.onNodeWithTag(OFFLINE_BANNER_TAG).getUnclippedBoundsInRoot()
        val bar = rule.onNodeWithTag(BOTTOM_BAR_TAG).getUnclippedBoundsInRoot()

        assertEquals(24f, banner.top.value, TOLERANCE)
        assertTrue(banner.bottom <= bar.top)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun tablet_bannerIsBesideRailAndUnderStatusBar() {
        rule.setContent { PocketCpApp(entries, online = false, windowInsets = fakeInsets) }

        val banner = rule.onNodeWithTag(OFFLINE_BANNER_TAG).getUnclippedBoundsInRoot()
        val rail = rule.onNodeWithTag(RAIL_TAG).getUnclippedBoundsInRoot()

        assertEquals(24f, banner.top.value, TOLERANCE)
        assertTrue(banner.left >= rail.right)
    }

    private companion object {
        /** Bounds are rounded to whole pixels. */
        const val TOLERANCE = 1f
    }
}
