package app.pocketcp.core.problemview

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.MaterialKind
import app.pocketcp.core.model.MaterialLink
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The WebView fallback and the Codeforces page over the statement, on a real [ProblemViewModel] with fakes. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
class ProblemWebViewTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val statements = FakeStatementRepository()
    private val settings = FakeSettingsRepository()
    private val opened = mutableListOf<String>()
    private val tutorial = "https://codeforces.com/blog/entry/150000"

    @Test
    fun unparsable_showsTheStatementsPage_inTheWebView_underTheNotice() {
        show { statements.row(Paco, StatementLanguage.En).value = cached(null, error = JudgeError.Malformed()) }

        rule.onNodeWithTag(PROBLEM_FALLBACK_TAG).assertExists()
        rule.onNodeWithTag(MATERIALS_MENU_TAG).assertDoesNotExist()
        assertEquals(statements.pageUrl(Paco, StatementLanguage.En), shadowOf(webView()).lastLoadedUrl)
    }

    @Test
    fun offline_keepsTheErrorState_withoutAWebView() {
        show { statements.row(Paco, StatementLanguage.En).value = cached(null, error = JudgeError.Offline()) }

        rule.onNodeWithTag(PROBLEM_FALLBACK_TAG).assertDoesNotExist()
        assertNull(shownWebView())
    }

    @Test
    fun materialTap_opensItOverTheStatement_andBackClosesIt() {
        statements.row(Paco, StatementLanguage.En).value = cached(withMaterials())
        show()
        assertNull(shownWebView())

        rule.onNodeWithTag(MATERIALS_MENU_TAG).performClick()
        rule.onNodeWithTag("problem_material_2").performClick()

        rule.onNodeWithTag(WEB_PAGE_TAG).assertExists()
        rule.onNodeWithTag("sample_1_input_copy").assertDoesNotExist()
        assertEquals(tutorial, shadowOf(webView()).lastLoadedUrl)

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(WEB_PAGE_TAG).assertDoesNotExist()
        rule.onNodeWithTag("sample_1_input_copy").assertExists()
        assertFalse(rule.activity.isFinishing)
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun theBackArrow_closesTheWebPage() {
        statements.row(Paco, StatementLanguage.En).value = cached(withMaterials())
        show()

        rule.onNodeWithTag(MATERIALS_MENU_TAG).performClick()
        rule.onNodeWithTag("problem_material_1").performClick()
        rule.onNodeWithTag(WEB_PAGE_TAG).assertExists()

        rule.onNodeWithContentDescription("Navigate back").performClick()
        rule.onNodeWithTag(WEB_PAGE_TAG).assertDoesNotExist()
        rule.onNodeWithTag(MATERIALS_MENU_TAG).assertExists()
    }

    private fun withMaterials() =
        shortStatement().copy(
            materials =
                listOf(
                    MaterialLink("Announcement", "https://codeforces.com/blog/entry/149000", MaterialKind.Announcement),
                    MaterialLink("Tutorial", tutorial, MaterialKind.Tutorial),
                ),
        )

    /** Shows the pane; [afterFetchStarts] runs once the view model has started its fetch (a failure lands then). */
    private fun show(afterFetchStarts: () -> Unit = {}) {
        val viewModel = ProblemViewModel(Paco, statements, settings, openUrl = { opened += it })
        afterFetchStarts()
        rule.setContent {
            PocketCpTheme(mode = ThemeMode.Dark) { ProblemPane(viewModel, onBack = null) }
        }
        rule.waitForIdle()
    }

    private fun webView(): WebView = checkNotNull(shownWebView()) { "No WebView in the hierarchy" }

    private fun shownWebView(): WebView? {
        val root: View = rule.activity.window.decorView
        return root.findWebView()
    }
}

private fun View.findWebView(): WebView? =
    when (this) {
        is WebView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findWebView() }
        else -> null
    }
