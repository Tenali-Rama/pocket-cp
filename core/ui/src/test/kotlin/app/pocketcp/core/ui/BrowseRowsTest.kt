package app.pocketcp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.Problem
import app.pocketcp.core.screenshot.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.ZoneOffset

/** What [ContestRow] and [ProblemRow] show, and what they hide. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [SCREENSHOT_SDK])
class BrowseRowsTest {
    @get:Rule
    val rule = createComposeRule()

    private val upcoming = SampleContests[0]
    private val running = SampleContests[1]
    private val finished = SampleContests[2]
    private val unknown = SampleContests[3]
    private val notAnnounced = SampleContests[4]

    @Test
    fun contestRowsShowTheirTimeAndPhaseLabel() {
        setContent { SampleContests.forEach { ContestRow(it, TestNow, onClick = {}) } }

        assertEquals(listOf(upcoming.name, "Starts in 2d 03:04:05", "Upcoming"), texts(contestTag(upcoming)))
        assertEquals(listOf(running.name, "Ends in 01:15:00", "Running"), texts(contestTag(running)))
        assertEquals(listOf(unknown.name, "Unknown"), texts(contestTag(unknown)))
        assertEquals(listOf(notAnnounced.name, "Start time not announced", "Upcoming"), texts(contestTag(notAnnounced)))
        val finishedTexts = texts(contestTag(finished))
        assertEquals(3, finishedTexts.size)
        assertTrue(finishedTexts[1], finishedTexts[1].startsWith("Oct 24, 2026"))
        assertTrue(finishedTexts[1], finishedTexts[1].endsWith(" · 02:15"))
        assertEquals("Finished", finishedTexts[2])
    }

    @Test
    fun finishedContestDateUsesTheClockZone() {
        // 2026-10-24T14:35Z is 23:35 in UTC+9, still the 24th; in UTC+10 it is already the 25th.
        setContent(clock = TestClock.withZone(ZoneOffset.ofHours(10))) {
            ContestRow(finished, TestNow, onClick = {})
        }

        assertTrue(texts(contestTag(finished))[1].startsWith("Oct 25, 2026"))
    }

    @Test
    fun problemRowShowsIdNameRatingTagsAndSolvedCount() {
        val problem = SampleProblems[0]
        setContent { ProblemRow(problem, hideTags = false, hideRating = false, highlighted = false, onClick = {}) }

        assertEquals(
            listOf("1900A · Cover in Water", "constructive algorithms, greedy", "800", "45,123 solved"),
            texts(problemTag(problem)),
        )
        rule.onNodeWithTag(problemTag(problem)).assertIsNotSelected()
    }

    @Test
    fun hiddenTagsAndRatingAreGoneFromTheAccessibilityTree() {
        val problem = SampleProblems[0]
        setContent { ProblemRow(problem, hideTags = true, hideRating = true, highlighted = false, onClick = {}) }

        val texts = texts(problemTag(problem))
        assertEquals(listOf("1900A · Cover in Water", "45,123 solved"), texts)
        assertFalse(texts.any { "greedy" in it || "800" in it })
        val config = rule.onNodeWithTag(problemTag(problem)).fetchSemanticsNode().config
        assertFalse(config.contains(SemanticsProperties.ContentDescription))
    }

    @Test
    fun hideRatingAloneKeepsTheTags() {
        val problem = SampleProblems[0]
        setContent { ProblemRow(problem, hideTags = false, hideRating = true, highlighted = false, onClick = {}) }

        assertEquals(
            listOf("1900A · Cover in Water", "constructive algorithms, greedy", "45,123 solved"),
            texts(problemTag(problem)),
        )
    }

    @Test
    fun highlightedRowIsSelected() {
        val problem = SampleProblems[1]
        setContent { ProblemRow(problem, hideTags = false, hideRating = false, highlighted = true, onClick = {}) }

        rule.onNodeWithTag(problemTag(problem)).assertIsSelected()
    }

    private fun setContent(
        clock: Clock = TestClock,
        content: @Composable () -> Unit,
    ) {
        rule.setContent { browseThemeWrapper(clock = clock)(true) { Column { content() } } }
    }

    private fun texts(tag: String): List<String> =
        rule
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .map { it.text }

    private fun contestTag(contest: Contest) = "contest_${contest.id.value}"

    private fun problemTag(problem: Problem) = "problem_${problem.id.display}"
}
