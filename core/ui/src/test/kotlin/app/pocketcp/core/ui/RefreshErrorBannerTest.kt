package app.pocketcp.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
class RefreshErrorBannerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyErrorHasItsOwnMessage() {
        val expected =
            listOf(
                JudgeError.Offline() to "Couldn't refresh: you're offline.",
                JudgeError.RateLimited() to "Couldn't refresh: too many requests. Try again in a moment.",
                JudgeError.Unavailable() to "Couldn't refresh: the server isn't responding.",
                JudgeError.Malformed() to "Couldn't refresh: the server sent an unexpected response.",
                JudgeError.Rejected("User with handle tourist2 not found") to "User with handle tourist2 not found",
            )
        var error: JudgeError by mutableStateOf(expected.first().first)
        rule.setContent { browseThemeWrapper()(true) { RefreshErrorBanner(error, onRetry = {}) } }

        expected.forEach { (case, message) ->
            error = case
            rule.onNodeWithText(message).assertExists("message for ${case::class.simpleName}")
        }
        assertEquals("one distinct message per case", expected.size, expected.map { it.second }.toSet().size)
    }

    @Test
    fun retryCallsOnRetry() {
        var retries = 0
        rule.setContent {
            browseThemeWrapper()(true) { RefreshErrorBanner(JudgeError.Offline(), onRetry = { retries++ }) }
        }

        rule.onNodeWithText("Retry").performClick()

        rule.runOnIdle { assertEquals(1, retries) }
    }
}
