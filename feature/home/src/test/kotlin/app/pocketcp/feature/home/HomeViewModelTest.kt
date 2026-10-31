package app.pocketcp.feature.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.pocketcp.core.data.Cached
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

private fun upcoming(
    id: Int,
    inSeconds: Long,
) = contest(id, ContestPhase.Upcoming, Now.plusSeconds(inSeconds))

private val endsLater = contest(1, ContestPhase.Running, Now.minusSeconds(1_800))
private val endsSooner = contest(2, ContestPhase.Running, Now.minusSeconds(3_600))
private val inAnHour = upcoming(3, 3_600)
private val inADay = upcoming(4, 86_400)
private val unscheduled = contest(5, ContestPhase.Upcoming, start = null)
private val finished = contest(6, ContestPhase.Finished, Now.minus(Duration.ofDays(1)))
private val unknown = contest(7, ContestPhase.Unknown, Now)

/** Unordered, with finished and unknown rows: Home still sections and limits whatever the repository emits. */
private val all = listOf(inADay, inAnHour, endsLater, endsSooner, unknown, finished, unscheduled)

private fun cached(
    data: List<Contest>? = all,
    refreshing: Boolean = false,
    error: JudgeError? = null,
) = Cached(data, fetchedAt = if (data == null) null else Now, refreshing = refreshing, error = error)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun sections_runningByEnd_upcomingSoonestFirst_finishedAndUnknownLeftOut() =
        runTest {
            newViewModel().state.test {
                val state = awaitUntil { it.status == HomeStatus.Loaded }
                assertEquals(
                    listOf(
                        HomeGroup(HomeSection.Running, listOf(endsSooner, endsLater)),
                        HomeGroup(HomeSection.Upcoming, listOf(inAnHour, inADay, unscheduled)),
                    ),
                    state.groups,
                )
            }
        }

    @Test
    fun upcoming_isLimitedToTheNextFive() =
        runTest {
            val later = (1..7).map { upcoming(100 + it, 3_600L * (8 - it)) }
            val repository = FakeContestRepository(cached(later))
            newViewModel(repository).state.test {
                val state = awaitUntil { it.status == HomeStatus.Loaded }
                assertEquals(listOf(5), repository.upcomingLimits)
                assertEquals(listOf(HomeSection.Upcoming), state.groups.map { it.section })
                assertEquals(listOf(107, 106, 105, 104, 103), state.contests().map { it.id.value })
            }
        }

    @Test
    fun emptySection_isLeftOut() =
        runTest {
            newViewModel(FakeContestRepository(cached(listOf(finished, inAnHour)))).state.test {
                val state = awaitUntil { it.status == HomeStatus.Loaded }
                assertEquals(listOf(HomeSection.Upcoming), state.groups.map { it.section })
            }
        }

    @Test
    fun noRunningOrUpcoming_isEmpty() =
        runTest {
            newViewModel(FakeContestRepository(cached(listOf(finished, unknown)))).state.test {
                val state = awaitUntil { it.status == HomeStatus.Empty }
                assertTrue(state.groups.isEmpty())
            }
        }

    @Test
    fun startCountdown_flipsLocally_andForcesARefresh_advancingClock() = runTest { assertFlipsAtStart(virtualClock()) }

    @Test
    fun startCountdown_flipsLocally_andForcesARefresh_fixedClock() = runTest { assertFlipsAtStart(FixedClock) }

    @Test
    fun countdownToEnd_ofARunningContest_triggersARefresh_andKeepsItRunningUntilTheJudgeSaysSo() =
        runTest {
            val endsSoon = contest(8, ContestPhase.Running, Now.minus(TwoHours).plusSeconds(60))
            val repository = FakeContestRepository(cached(listOf(endsSoon)))
            val viewModel = newViewModel(repository, virtualClock())
            viewModel.state.test {
                awaitUntil { it.status == HomeStatus.Loaded }
                advanceTimeBy(59_999)
                assertTrue(repository.refreshes.isEmpty())

                advanceTimeBy(2)
                assertEquals(listOf(false to true), repository.refreshes)
                // Same sections, so the state flow emits nothing new.
                expectNoEvents()
                assertEquals(listOf(HomeGroup(HomeSection.Running, listOf(endsSoon))), viewModel.state.value.groups)
            }
        }

    @Test
    fun cachedData_whileRefreshing_isLoaded_withoutAnIndicator() =
        runTest {
            newViewModel(FakeContestRepository(cached(refreshing = true))).state.test {
                val state = awaitUntil { it.status == HomeStatus.Loaded }
                assertFalse(state.userRefreshing)
                assertNull(state.error)
            }
        }

    @Test
    fun cachedData_withError_isLoaded_withTheError() =
        runTest {
            val error = JudgeError.Offline()
            newViewModel(FakeContestRepository(cached(error = error))).state.test {
                val state = awaitUntil { it.status == HomeStatus.Loaded }
                assertEquals(error, state.error)
                assertEquals(2, state.groups.size)
            }
        }

    @Test
    fun noData_withError_isError_andRetryingIsLoading() =
        runTest {
            val error = JudgeError.Unavailable()
            val repository = FakeContestRepository(cached(data = null, error = error))
            newViewModel(repository).state.test {
                assertEquals(error, awaitUntil { it.status == HomeStatus.Error }.error)

                repository.cached.value = cached(data = null, refreshing = true, error = error)
                awaitUntil { it.status == HomeStatus.Loading }
            }
        }

    @Test
    fun noData_whileRefreshing_isLoading_untilDataArrives() =
        runTest {
            val repository = FakeContestRepository(cached(data = null, refreshing = true))
            newViewModel(repository).state.test {
                assertTrue(awaitUntil { it.status == HomeStatus.Loading }.groups.isEmpty())
                repository.cached.value = cached()
                awaitUntil { it.status == HomeStatus.Loaded }
            }
        }

    @Test
    fun refresh_forcesARegularContestRefresh_andShowsTheIndicatorUntilDone() =
        runTest {
            val repository = FakeContestRepository(cached())
            val gate = CompletableDeferred<Unit>()
            repository.refreshGate = gate
            val viewModel = newViewModel(repository)
            viewModel.state.test {
                viewModel.refresh()
                awaitUntil { it.userRefreshing }
                viewModel.refresh() // ignored while one runs
                assertEquals(listOf(false to true), repository.refreshes)

                gate.complete(Unit)
                awaitUntil { !it.userRefreshing }
            }
        }

    /** One contest ten minutes away: Upcoming with no refresh, then Running and exactly one forced refresh. */
    private suspend fun TestScope.assertFlipsAtStart(clock: Clock) {
        val soon = upcoming(9, 600)
        val repository = FakeContestRepository(cached(listOf(soon)))
        newViewModel(repository, clock).state.test {
            val before = awaitUntil { it.status == HomeStatus.Loaded }
            assertEquals(HomeSection.Upcoming, before.groups.single().section)
            assertTrue(repository.refreshes.isEmpty())

            val after = awaitItem()
            assertEquals(HomeSection.Running, after.groups.single().section)
            assertEquals(ContestPhase.Running, after.contests().single().phase)
            assertEquals(Duration.ofSeconds(600).toMillis(), testScheduler.currentTime)
            assertEquals(listOf(false to true), repository.refreshes)
        }
    }

    /** The view model on the test's main dispatcher, so its sectioning runs on virtual time. */
    private fun newViewModel(
        repository: FakeContestRepository = FakeContestRepository(cached()),
        clock: Clock = FixedClock,
    ) = HomeViewModel(repository, clock, Dispatchers.Main)

    /** A clock at [Now] plus the test's virtual time. */
    private fun TestScope.virtualClock(): Clock =
        object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId): Clock = this

            override fun instant(): Instant = Now.plusMillis(testScheduler.currentTime)
        }
}

private fun HomeUiState.contests(): List<Contest> = groups.flatMap { it.contests }

/** Skips states until one matches [predicate]: the first item may be the initial state before the data arrives. */
private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
    while (true) {
        val state = awaitItem()
        if (predicate(state)) return state
    }
}
