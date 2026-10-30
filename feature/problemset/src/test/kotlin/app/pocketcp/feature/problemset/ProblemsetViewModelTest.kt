package app.pocketcp.feature.problemset

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.pocketcp.core.data.Cached
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

private val FetchedAt: Instant = Instant.parse("2026-10-31T12:00:00Z")

private fun problem(
    contestId: Int,
    index: String,
    name: String,
    rating: Int?,
    solved: Int,
) = Problem(ProblemId(contestId, index), name, rating, listOf("math"), solved)

private val p1901b = problem(1901, "B", "Zebra Tiles", rating = 1500, solved = 900)
private val p1900a = problem(1900, "A", "Cover in Water", rating = 800, solved = 30_000)
private val p1900b = problem(1900, "B", "Laura and Operations", rating = null, solved = 20_000)
private val p1899c = problem(1899, "C", "Bad Water", rating = 1200, solved = 10)

/** In repository order: newest contest first, then by index. */
private val all = listOf(p1901b, p1900a, p1900b, p1899c)

private fun cached(
    data: List<Problem>? = all,
    refreshing: Boolean = false,
    error: JudgeError? = null,
) = Cached(data, fetchedAt = data?.let { FetchedAt }, refreshing = refreshing, error = error)

@OptIn(ExperimentalCoroutinesApi::class)
class ProblemsetViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loaded_showsEveryProblemNewestFirst_withDefaults() =
        runTest {
            newViewModel().state.test {
                val state = awaitUntil { it.status == ProblemsetStatus.Loaded }
                assertEquals(all, state.problems)
                assertEquals(ProblemsetOptions(), state.options)
            }
        }

    @Test
    fun search_byContestId_byProblemId_andByName() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                viewModel.search("1900")
                assertEquals("1900", viewModel.searchText)
                assertEquals(listOf(p1900a, p1900b), awaitUntil { it.options.query == "1900" }.problems)

                viewModel.search("1900b")
                assertEquals(listOf(p1900b), awaitUntil { it.options.query == "1900b" }.problems)

                viewModel.search("water")
                assertEquals(listOf(p1900a, p1899c), awaitUntil { it.options.query == "water" }.problems)
            }
        }

    @Test
    fun searchWithoutMatches_isEmpty() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                viewModel.search("2000")
                val state = awaitUntil { it.options.query == "2000" }
                assertEquals(ProblemsetStatus.Empty, state.status)
                assertTrue(state.problems.isEmpty())
            }
        }

    @Test
    fun eachSort_ordersTheList() =
        runTest {
            val viewModel = newViewModel()
            val expected =
                mapOf(
                    ProblemSort.RatingAscending to listOf(p1900a, p1899c, p1901b, p1900b),
                    ProblemSort.RatingDescending to listOf(p1901b, p1899c, p1900a, p1900b),
                    ProblemSort.MostSolved to listOf(p1900a, p1900b, p1901b, p1899c),
                    ProblemSort.Name to listOf(p1899c, p1900a, p1900b, p1901b),
                    ProblemSort.Newest to all,
                )
            viewModel.state.test {
                expected.forEach { (sort, problems) ->
                    viewModel.sortBy(sort)
                    assertEquals(sort.name, problems, awaitUntil { it.options.sort == sort }.problems)
                }
            }
        }

    @Test
    fun hideTags_isWrittenToSettings_andApplied() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = newViewModel(settings = settings)
            viewModel.state.test {
                assertFalse(awaitUntil { it.status == ProblemsetStatus.Loaded }.options.hideTags)

                viewModel.setHideTags(true)
                assertTrue(settings.stored.value.hideProblemTags)
                assertTrue(awaitUntil { it.options.hideTags }.problems.isNotEmpty())

                viewModel.setHideTags(false)
                assertFalse(settings.stored.value.hideProblemTags)
                awaitUntil { !it.options.hideTags }
            }
        }

    @Test
    fun hideRatings_isWrittenToSettings_andDropsTheRatingSorts() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = newViewModel(settings = settings)
            viewModel.state.test {
                viewModel.sortBy(ProblemSort.RatingDescending)
                awaitUntil { it.options.effectiveSort == ProblemSort.RatingDescending }

                viewModel.setHideRatings(true)
                assertTrue(settings.stored.value.hideProblemRatings)
                val hidden = awaitUntil { it.options.hideRatings }
                assertEquals(ProblemSort.Newest, hidden.options.effectiveSort)
                assertEquals(all, hidden.problems)
                assertEquals(
                    listOf(ProblemSort.Newest, ProblemSort.MostSolved, ProblemSort.Name),
                    hidden.options.sortChoices,
                )

                // Showing ratings again brings the chosen order back.
                viewModel.setHideRatings(false)
                val shown = awaitUntil { !it.options.hideRatings }
                assertEquals(ProblemSort.RatingDescending, shown.options.effectiveSort)
                assertEquals(ProblemSort.entries, shown.options.sortChoices)
            }
        }

    @Test
    fun storedSpoilerSettings_areAppliedFromTheFirstLoadedState() =
        runTest {
            val settings = FakeSettingsRepository(UserSettings(hideProblemTags = true, hideProblemRatings = true))
            newViewModel(settings = settings).state.test {
                val state = awaitUntil { it.status != ProblemsetStatus.Loading }
                assertTrue(state.options.hideTags)
                assertTrue(state.options.hideRatings)
            }
        }

    @Test
    fun queryAndSort_areWrittenTo_andRestoredFrom_savedState() =
        runTest {
            val handle = SavedStateHandle()
            val first = newViewModel(handle = handle)
            first.search("1900")
            first.sortBy(ProblemSort.MostSolved)

            // Process death: a new view model over the handle's saved values.
            val restored = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })
            val viewModel = newViewModel(handle = restored)
            assertEquals("1900", viewModel.searchText)
            viewModel.state.test {
                val state = awaitUntil { it.status == ProblemsetStatus.Loaded }
                assertEquals(ProblemsetOptions(query = "1900", sort = ProblemSort.MostSolved), state.options)
                assertEquals(listOf(p1900a, p1900b), state.problems)
            }
        }

    @Test
    fun cachedData_whileRefreshing_isLoaded_withoutAnIndicator() =
        runTest {
            newViewModel(FakeProblemRepository(cached(refreshing = true))).state.test {
                val state = awaitUntil { it.status == ProblemsetStatus.Loaded }
                assertFalse(state.userRefreshing)
                assertNull(state.error)
            }
        }

    @Test
    fun cachedData_withError_isLoaded_withTheError() =
        runTest {
            val error = JudgeError.Offline()
            newViewModel(FakeProblemRepository(cached(error = error))).state.test {
                val state = awaitUntil { it.status == ProblemsetStatus.Loaded }
                assertEquals(error, state.error)
                assertEquals(all, state.problems)
            }
        }

    @Test
    fun noData_withError_isError_andRetryingIsLoading() =
        runTest {
            val error = JudgeError.Unavailable()
            val repository = FakeProblemRepository(cached(data = null, error = error))
            newViewModel(repository).state.test {
                assertEquals(error, awaitUntil { it.status == ProblemsetStatus.Error }.error)

                repository.cached.value = cached(data = null, refreshing = true, error = error)
                awaitUntil { it.status == ProblemsetStatus.Loading }
            }
        }

    @Test
    fun noData_whileRefreshing_isLoading_untilTheDataArrives() =
        runTest {
            val repository = FakeProblemRepository(cached(data = null, refreshing = true))
            newViewModel(repository).state.test {
                assertTrue(awaitUntil { it.status == ProblemsetStatus.Loading }.problems.isEmpty())
                repository.cached.value = cached()
                awaitUntil { it.status == ProblemsetStatus.Loaded }
            }
        }

    @Test
    fun emptyProblemset_isEmpty() =
        runTest {
            newViewModel(FakeProblemRepository(cached(data = emptyList()))).state.test {
                awaitUntil { it.status == ProblemsetStatus.Empty }
            }
        }

    @Test
    fun refresh_forcesARefresh_andShowsTheIndicatorUntilDone() =
        runTest {
            val repository = FakeProblemRepository(cached())
            val gate = CompletableDeferred<Unit>()
            repository.refreshGate = gate
            val viewModel = newViewModel(repository)
            viewModel.state.test {
                viewModel.refresh()
                awaitUntil { it.userRefreshing }
                viewModel.refresh() // ignored while one runs
                assertEquals(listOf(true), repository.refreshes)

                gate.complete(Unit)
                awaitUntil { !it.userRefreshing }
            }
        }

    /** The view model on the test's main dispatcher, so its searching runs on the test scheduler. */
    private fun newViewModel(
        repository: FakeProblemRepository = FakeProblemRepository(cached()),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        handle: SavedStateHandle = SavedStateHandle(),
    ) = ProblemsetViewModel(repository, settings, handle, Dispatchers.Main)
}

/** Skips states until one matches [predicate]: the first item may be the initial state before the data arrives. */
private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
    while (true) {
        val state = awaitItem()
        if (predicate(state)) return state
    }
}
