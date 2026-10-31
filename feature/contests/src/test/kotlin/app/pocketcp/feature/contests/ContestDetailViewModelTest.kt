package app.pocketcp.feature.contests

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.pocketcp.core.data.Cached
import app.pocketcp.core.datastore.UserSettings
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private val Now: Instant = Instant.parse("2026-10-31T12:00:00Z")
private val FixedClock: Clock = Clock.fixed(Now, ZoneOffset.UTC)
private val TwoHours: Duration = Duration.ofHours(2)

private fun contest(
    id: Int,
    phase: ContestPhase,
    start: Instant?,
) = Contest(ContestId(id), "Codeforces Round $id (Div. 2)", phase, start, TwoHours, isGym = false)

private val upcoming = contest(1, ContestPhase.Upcoming, Now.plusSeconds(3_600))
private val running = contest(2, ContestPhase.Running, Now.minusSeconds(1_800))
private val finished = contest(3, ContestPhase.Finished, Now.minus(Duration.ofDays(2)))
private val started = contest(4, ContestPhase.Upcoming, Now.minusSeconds(600))
private val unscheduled = contest(5, ContestPhase.Upcoming, start = null)
private val contests = listOf(upcoming, running, finished, started, unscheduled)

private fun problem(
    contest: Int,
    index: String,
) = Problem(ProblemId(contest, index), "Problem $contest$index", rating = 1_500, tags = listOf("math"), solvedCount = 9)

/** Contest 3's problems out of order, between other contests' problems, as the problemset may hold them. */
private val problemset =
    listOf(
        problem(3, "C"),
        problem(30, "A"),
        problem(3, "B1"),
        problem(3, "A"),
        problem(2, "A"),
        problem(3, "B"),
    )

private fun <T> cached(
    data: T?,
    refreshing: Boolean = false,
    error: JudgeError? = null,
) = Cached(data, fetchedAt = data?.let { Now }, refreshing = refreshing, error = error)

@OptIn(ExperimentalCoroutinesApi::class)
class ContestDetailViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val contestRepository = FakeContestRepository(cached(contests))
    private val problemRepository = FakeProblemRepository(cached(problemset))
    private val settingsRepository = FakeSettingsRepository()

    @Test
    fun header_upcoming_countsDownToItsStart() = assertTiming(upcoming, ContestTiming.StartsIn(upcoming.start!!))

    @Test
    fun header_running_countsDownToItsEnd() {
        assertTiming(running, ContestTiming.EndsIn(running.start!!.plus(TwoHours)))
    }

    @Test
    fun header_finished_showsItsStartAndLength() {
        assertTiming(finished, ContestTiming.Held(finished.start!!, TwoHours))
    }

    @Test
    fun header_upcomingWhoseStartPassed_isRunningAsOfTheClock() =
        runTest {
            val state = shownState(started)
            assertEquals(ContestPhase.Running, state.contest?.phase)
            assertEquals(ContestTiming.EndsIn(started.start!!.plus(TwoHours)), state.timing)
        }

    @Test
    fun header_withoutAStart_isNotAnnounced() = assertTiming(unscheduled, ContestTiming.NotAnnounced)

    @Test
    fun header_upcomingContest_becomesRunning_whenItsStartPasses() =
        runTest {
            val soon = contest(9, ContestPhase.Upcoming, Now.plusSeconds(600))
            contestRepository.cached.value = cached(listOf(soon))
            val viewModel = newViewModel(clock = virtualClock())
            viewModel.open(soon.id)
            viewModel.state.test {
                assertEquals(ContestPhase.Upcoming, awaitUntil { it.contest != null }.contest?.phase)
                val now = awaitUntil { it.contest?.phase == ContestPhase.Running }
                assertEquals(ContestTiming.EndsIn(soon.start!!.plus(TwoHours)), now.timing)
            }
        }

    @Test
    fun problems_areTheContestsOnly_orderedByIndex() =
        runTest {
            val state = shownState(finished)
            assertEquals(ProblemsStatus.Loaded, state.problemsStatus)
            assertEquals(listOf("3A", "3B", "3B1", "3C"), state.problems.map { it.id.display })
        }

    @Test
    fun noProblems_beforeTheContestFinishes_isNotYetAdded() =
        runTest {
            assertEquals(ProblemsStatus.NotYetAdded, shownState(upcoming).problemsStatus)
        }

    @Test
    fun noProblems_forAFinishedContest_isEmpty() =
        runTest {
            problemRepository.cached.value = cached(listOf(problem(2, "A")))
            val state = shownState(finished)
            assertEquals(ProblemsStatus.Empty, state.problemsStatus)
            assertTrue(state.problems.isEmpty())
        }

    @Test
    fun noCachedProblemset_isLoading_andWithAFailedRefresh_isError() =
        runTest {
            problemRepository.cached.value = cached(null, refreshing = true)
            assertEquals(ProblemsStatus.Loading, shownState(finished).problemsStatus)

            val error = JudgeError.Offline()
            problemRepository.cached.value = cached(null, error = error)
            val failed = shownState(finished)
            assertEquals(ProblemsStatus.Error, failed.problemsStatus)
            assertEquals(error, failed.problemsError)
        }

    @Test
    fun spoilerFlags_comeFromTheSettings() =
        runTest {
            settingsRepository.stored.value = UserSettings(hideProblemTags = true, hideProblemRatings = true)
            val state = shownState(finished)
            assertTrue(state.hideTags)
            assertTrue(state.hideRatings)
        }

    @Test
    fun unknownId_isNotFound() =
        runTest {
            val viewModel = newViewModel()
            viewModel.open(ContestId(404))
            viewModel.state.test { awaitUntil { it.status == DetailStatus.NotFound } }
        }

    @Test
    fun withoutAnOpenedContest_theDefaultIsShown_untilOneIsOpened() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                assertEquals(DetailStatus.None, awaitItem().status)

                viewModel.showByDefault(upcoming.id)
                val default = awaitUntil { it.contest != null }
                assertEquals(upcoming.id, default.shown)
                assertNull(default.selected)

                viewModel.open(finished.id)
                val opened = awaitUntil { it.contest?.id == finished.id }
                assertEquals(finished.id, opened.selected)
            }
        }

    @Test
    fun openedContest_isRestoredAfterProcessDeath_andCloseIsToo() =
        runTest {
            val handle = SavedStateHandle()
            newViewModel(handle).open(finished.id)

            val restored = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })
            val viewModel = newViewModel(restored)
            viewModel.state.test {
                val state = awaitUntil { it.contest != null }
                assertEquals(finished.id, state.selected)
                assertEquals(finished.id, state.contest?.id)

                viewModel.close()
                assertNull(awaitUntil { it.selected == null }.shown)
            }
            val closed = SavedStateHandle(restored.keys().associateWith { restored.get<Any>(it) })
            newViewModel(closed)
            assertNull(closed.get<Int>(KEY_SELECTED))
        }

    @Test
    fun routeArgument_opensItsContest_butASavedCloseWins() =
        runTest {
            val viewModel = newViewModel(SavedStateHandle(mapOf(ARG_ID to running.id.value)))
            viewModel.state.test { assertEquals(running.id, awaitUntil { it.contest != null }.selected) }

            // Closed before process death: the route's argument doesn't reopen it, so nothing is shown or fetched.
            val prefetches = problemRepository.refreshes.size
            val closed = SavedStateHandle(mapOf(ARG_ID to running.id.value, KEY_SELECTED to null))
            newViewModel(closed)
            assertNull(closed.get<Int>(KEY_SELECTED))
            assertEquals(prefetches, problemRepository.refreshes.size)
        }

    @Test
    fun showingAContest_prefetchesTheProblemset_unforced_onceEach() =
        runTest {
            val viewModel = newViewModel()
            assertTrue(problemRepository.refreshes.isEmpty())

            viewModel.showByDefault(upcoming.id)
            viewModel.open(upcoming.id) // same contest: no second prefetch
            viewModel.open(finished.id)
            assertEquals(listOf(false, false), problemRepository.refreshes)

            viewModel.retryProblems()
            assertEquals(listOf(false, false, true), problemRepository.refreshes)
        }

    private fun assertTiming(
        contest: Contest,
        expected: ContestTiming,
    ) = runTest { assertEquals(expected, shownState(contest).timing) }

    /** The first loaded state with [contest] opened in a new view model. */
    private suspend fun shownState(contest: Contest): ContestDetailUiState {
        val viewModel = newViewModel()
        viewModel.open(contest.id)
        var state = ContestDetailUiState()
        viewModel.state.test { state = awaitUntil { it.status == DetailStatus.Loaded } }
        return state
    }

    /** The view model on the test's main dispatcher, so its phase changes run on virtual time. */
    private fun newViewModel(
        handle: SavedStateHandle = SavedStateHandle(),
        clock: Clock = FixedClock,
    ) = ContestDetailViewModel(
        contestRepository,
        problemRepository,
        settingsRepository,
        handle,
        clock,
        Dispatchers.Main,
    )

    /** A clock at [Now] plus the test's virtual time. */
    private fun TestScope.virtualClock(): Clock =
        object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId): Clock = this

            override fun instant(): Instant = Now.plusMillis(testScheduler.currentTime)
        }
}
