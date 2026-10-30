package app.pocketcp.feature.settings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.adaptive.WindowWidthClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The About page: the version comes from the installed package, and TalkBack can read the disclaimer. */
@RunWith(AndroidJUnit4::class)
class AboutTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName).versionName = "9.8.7"
    }

    @Test
    fun installedAppInfo_readsTheVersionFromThePackage() {
        assertEquals("9.8.7", context.installedAppInfo().versionName)
    }

    @Test
    fun aboutPage_showsThePackageVersion() {
        showAbout()

        rule.onNodeWithTag("settings_about_version").assertTextEquals("Version 9.8.7")
    }

    @Test
    fun disclaimer_isPresentAndReadableByTalkBack() {
        val disclaimer = context.getString(R.string.settings_about_disclaimer)
        showAbout()

        rule
            .onNodeWithText(disclaimer)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.HideFromAccessibility))
        rule.onNodeWithTag("settings_about_disclaimer").assertTextEquals(disclaimer)
    }

    private fun showAbout() {
        val viewModel = SettingsViewModel(FakeSettingsRepository(), emptySet())
        rule.setContent {
            Themed(WindowWidthClass.Compact) {
                val state by viewModel.state.collectAsState()
                SettingsRoute(state, viewModel)
            }
        }
        rule.onNodeWithTag("settings_category_about").performClick()
    }
}
