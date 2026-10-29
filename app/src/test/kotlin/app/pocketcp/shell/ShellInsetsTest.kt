package app.pocketcp.shell

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.navigation.inNavigationOrder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Edge-to-edge: the bar and rail pad themselves by the system-bar insets (faked: 48 dp bottom, 24 dp start). */
@RunWith(AndroidJUnit4::class)
class ShellInsetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val entries = PlaceholderModule.placeholders().inNavigationOrder()
    private val fakeInsets = WindowInsets(left = 24.dp, bottom = 48.dp)

    @Test
    @Config(qualifiers = "w411dp-h914dp")
    fun phone_bottomBarBottomPadding_isNavigationBarInset() {
        rule.setContent { PocketCpApp(entries, windowInsets = fakeInsets) }

        val bar = rule.onNodeWithTag(BOTTOM_BAR_TAG).getUnclippedBoundsInRoot()
        val items = rule.onNodeWithTag(BOTTOM_BAR_ITEMS_TAG).getUnclippedBoundsInRoot()

        assertEquals(48f, (bar.bottom - items.bottom).value, TOLERANCE)
        rule.onNodeWithTag(RAIL_TAG).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun tablet_railStartPadding_isSystemBarInset() {
        rule.setContent { PocketCpApp(entries, windowInsets = fakeInsets) }

        val rail = rule.onNodeWithTag(RAIL_TAG).getUnclippedBoundsInRoot()
        val items = rule.onNodeWithTag(RAIL_ITEMS_TAG).getUnclippedBoundsInRoot()

        assertEquals(24f, (items.left - rail.left).value, TOLERANCE)
        rule.onNodeWithTag(BOTTOM_BAR_TAG).assertDoesNotExist()
    }

    private companion object {
        /** Bounds are rounded to whole pixels. */
        const val TOLERANCE = 1f
    }
}
