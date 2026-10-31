package app.pocketcp.feature.problemset

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.pocketcp.core.adaptive.DevicePosture
import app.pocketcp.core.adaptive.LocalWindowLayout
import app.pocketcp.core.adaptive.WindowHeightClass
import app.pocketcp.core.adaptive.WindowLayout
import app.pocketcp.core.adaptive.WindowWidthClass
import app.pocketcp.core.adaptive.navigationTypeOf
import app.pocketcp.core.data.Cached
import app.pocketcp.core.designsystem.PocketCpTheme
import app.pocketcp.core.designsystem.ThemeMode
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The screen's wiring to [ProblemsetActions], over `problemset.problems.json`. */
@RunWith(AndroidJUnit4::class)
class ProblemsetScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private val actions =
        object : ProblemsetActions {
            override fun search(text: String) {
                calls += "search $text"
            }

            override fun sortBy(sort: ProblemSort) {
                calls += "sort ${sort.name}"
            }

            override fun setHideTags(hidden: Boolean) {
                calls += "hideTags $hidden"
            }

            override fun setHideRatings(hidden: Boolean) {
                calls += "hideRatings $hidden"
            }

            override fun toggleTag(tag: String) {
                calls += "tag $tag"
            }

            override fun toggleDivision(division: ContestDivision) {
                calls += "division ${division.name}"
            }

            override fun setRatingRange(
                min: Int,
                max: Int,
            ) {
                calls += "rating $min-$max"
            }

            override fun clearFilters() {
                calls += "clearFilters"
            }

            override fun pickRandom() {
                calls += "random"
            }

            override fun refresh() {
                calls += "refresh"
            }
        }

    @Test
    fun rows_showTagsAndRatings() {
        show(WindowWidthClass.Compact)
        rule.onNodeWithTag("problem_2276F").assertTextContains("dsu", substring = true)
        rule.onNodeWithTag("problem_2276F").assertTextContains("2400", substring = true)
    }

    @Test
    fun hiddenSpoilers_dropTagsAndRatingsFromEveryRow() {
        show(WindowWidthClass.Compact, options = ProblemsetOptions(hideTags = true, hideRatings = true))
        rule.onNodeWithText("dsu", substring = true).assertDoesNotExist()
        rule.onNodeWithText("2400").assertDoesNotExist()
        rule.onNodeWithTag("problem_2276F").assertTextContains("Permutation Coloring Game", substring = true)
    }

    @Test
    fun typing_searches() {
        show(WindowWidthClass.Compact)
        rule.onNodeWithTag("problemset_search").performTextInput("2274")
        assertEquals(listOf("search 2274"), calls)
    }

    @Test
    fun sortChips_sort() = sorts(WindowWidthClass.Compact)

    @Test
    fun sortPane_sorts() = sorts(WindowWidthClass.Expanded)

    @Test
    fun hiddenRatings_offerNoRatingSort() {
        show(WindowWidthClass.Compact, options = ProblemsetOptions(hideRatings = true))
        rule.onNodeWithTag(ProblemSort.RatingAscending.testTag).assertDoesNotExist()
        rule.onNodeWithTag(ProblemSort.RatingDescending.testTag).assertDoesNotExist()
        rule.onNodeWithTag(ProblemSort.MostSolved.testTag).assertExists()
    }

    @Test
    fun overflowMenu_togglesTagsAndRatings_showingTheirState() {
        show(WindowWidthClass.Compact, options = ProblemsetOptions(hideRatings = true))
        rule.onNodeWithTag("problemset_menu").performClick()
        rule.onNodeWithTag("problemset_hide_tags").assertIsOff().performClick()
        rule.onNodeWithTag("problemset_menu").performClick()
        rule.onNodeWithTag("problemset_hide_ratings").assertIsOn().performClick()
        assertEquals(listOf("hideTags true", "hideRatings false"), calls)
    }

    @Test
    fun refreshButton_refreshes() {
        show(WindowWidthClass.Compact)
        rule.onNodeWithContentDescription("Refresh problemset").performClick()
        assertEquals(listOf("refresh"), calls)
    }

    @Test
    fun errorState_retryRefreshes() {
        show(WindowWidthClass.Compact, data = null, error = JudgeError.Offline())
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("refresh"), calls)
    }

    @Test
    fun noMatches_clearTheSearch() {
        show(WindowWidthClass.Compact, options = ProblemsetOptions(query = "zzz"))
        rule.onNodeWithText("Clear search").performClick()
        assertEquals(listOf("search "), calls)
    }

    @Test
    fun filterButton_showsTheCount_andOpensTheSheet_whoseChipsFilter() {
        val filter = ProblemFilter(tags = setOf("dp"), ratingMin = 1200, divisions = setOf(ContestDivision.Div2))
        show(WindowWidthClass.Compact, options = ProblemsetOptions(filter = filter))
        rule.onNodeWithTag("problemset_filter_sheet").assertDoesNotExist()
        rule.onNodeWithTag("problemset_filters").assertTextContains("Filters · 3").performClick()

        rule.onNodeWithTag(tagTestTag("dsu")).performScrollTo().performClick()
        rule.onNodeWithTag(ContestDivision.Educational.testTag).performScrollTo().performClick()
        rule.onNodeWithTag("problemset_filters_clear").performClick()
        rule.onNodeWithTag("problemset_filters_done").performClick()
        rule.onNodeWithTag("problemset_filter_sheet").assertDoesNotExist()
        assertEquals(listOf("tag dsu", "division Educational", "clearFilters"), calls)
    }

    @Test
    fun expandedWidth_showsTheFilterPanel_withoutAFilterButton() {
        show(WindowWidthClass.Expanded)
        rule.onNodeWithTag("problemset_filters").assertDoesNotExist()
        rule.onNodeWithTag("problemset_filters_done").assertDoesNotExist()
        rule.onNodeWithTag(tagTestTag("brute force")).performScrollTo().performClick()
        assertEquals(listOf("tag brute force"), calls)
    }

    @Test
    fun hiddenTags_collapseTheTagFilter_untilShown() {
        show(WindowWidthClass.Expanded, options = ProblemsetOptions(hideTags = true))
        rule.onNodeWithTag(tagTestTag("dsu")).assertDoesNotExist()
        rule.onNodeWithTag("problemset_show_tags").performScrollTo().performClick()
        rule.onNodeWithTag(tagTestTag("dsu")).assertExists()
    }

    @Test
    fun random_picks() {
        show(WindowWidthClass.Compact)
        rule.onNodeWithTag("problemset_random").assertIsEnabled().performClick()
        assertEquals(listOf("random"), calls)
    }

    @Test
    fun random_isDisabled_whenTheListIsEmpty() {
        show(WindowWidthClass.Compact, options = ProblemsetOptions(filter = ProblemFilter(tags = setOf("graphs"))))
        rule.onNodeWithTag("problemset_random").assertIsNotEnabled()
        rule.onNodeWithText("No problems match these filters").assertExists()
        rule.onNodeWithText("Clear filters").performClick()
        assertEquals(listOf("clearFilters"), calls)
    }

    @Test
    fun pickedProblem_isHighlighted() {
        show(WindowWidthClass.Compact, pick = RandomPick("2274A", count = 1))
        rule.onNodeWithTag("problem_2274A").assertIsSelected()
        rule.onNodeWithTag("problem_2276F").assertIsNotSelected()
    }

    private fun sorts(width: WindowWidthClass) {
        show(width)
        rule.onNodeWithTag(ProblemSort.MostSolved.testTag).performScrollTo().performClick()
        rule.onNodeWithTag(ProblemSort.RatingDescending.testTag).performScrollTo().performClick()
        assertEquals(listOf("sort MostSolved", "sort RatingDescending"), calls)
    }

    private fun show(
        width: WindowWidthClass,
        data: List<Problem>? = FixtureProblems,
        error: JudgeError? = null,
        options: ProblemsetOptions = ProblemsetOptions(),
        pick: RandomPick = RandomPick(),
    ) {
        val cached = Cached(data, fetchedAt = null, refreshing = false, error = error)
        val state = problemsetUiState(cached, options, userRefreshing = false, pick = pick)
        val layout = WindowLayout(width, WindowHeightClass.Medium, DevicePosture.Flat, navigationTypeOf(width))
        rule.setContent {
            PocketCpTheme(ThemeMode.Dark) {
                CompositionLocalProvider(LocalWindowLayout provides layout) {
                    ProblemsetScreen(state, options.query, actions)
                }
            }
        }
    }
}
