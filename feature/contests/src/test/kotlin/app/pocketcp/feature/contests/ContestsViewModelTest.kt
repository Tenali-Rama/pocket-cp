package app.pocketcp.feature.contests

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.pocketcp.core.data.Cached
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestDivision
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
    name: String,
    phase: ContestPhase,
    start: Instant?,
    kind: ContestKind = ContestKind.Cf,
) = Contest(ContestId(id), name, phase, start, TwoHours, isGym = false, kind = kind)

private val running1 = contest(1, "Codeforces Round 1 (Div. 1)", ContestPhase.Running, Now.minusSeconds(3_600))
private val running2 = contest(2, "Codeforces Round 2 (Div. 2)", ContestPhase.Running, Now.minusSeconds(1_800))
private val educational =
    contest(
        3,
        "Educational Codeforces Round 3 (Rated for Div. 2)",
        ContestPhase.Upcoming,
        Now.plus(Duration.ofDays(2)),
        ContestKind.Icpc,
    )
private val div3 =
    contest(4, "Codeforces Round 4 (Div. 3)", ContestPhase.Upcoming, Now.plusSeconds(3_600), ContestKind.Icpc)
private val unscheduled = contest(5, "Codeforces Round 5 (Div. 2)", ContestPhase.Upcoming, start = null)
private val past1 = contest(6, "Codeforces Round 6 (Div. 2)", ContestPhase.Finished, Now.minus(Duration.ofDays(3)))
private val past2 = contest(7, "Codeforces Global Round 7", ContestPhase.Finished, Now.minus(Duration.ofDays(1)))
private val unknown = contest(8, "Codeforces Round 8 (Div. 2)", ContestPhase.Unknown, Now, ContestKind.Unknown)

/** In repository order (newest start first, no start last), as `ContestRepository` emits them. */
private val all = listOf(educational, div3, running2, running1, past2, unknown, past1, unscheduled)

private fun cached(
    data: List<Contest>? = all,
    refreshing: Boolean = false,
    error: JudgeError? = null,
) = Cached(data, fetchedAt = if (data == null) null else Now, refreshing = refreshing, error = error)

@OptIn(ExperimentalCoroutinesApi::class)
class ContestsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun sections_runningByEnd_upcomingSoonestFirst_pastNewestFirst_unknownHidden() =
        runTest {
            newViewModel().state.test {
                val state = awaitUntil { it.status == ContestsStatus.Loaded }
                assertEquals(
                    listOf(
                        ContestGroup(ContestSection.Running, listOf(running1, running2)),
                        ContestGroup(ContestSection.Upcoming, listOf(div3, educational, unscheduled)),
                        ContestGroup(ContestSection.Past, listOf(past2, past1)),
                    ),
                    state.groups,
                )
            }
        }

    @Test
    fun emptySections_areLeftOut() =
        runTest {
            val repository = FakeContestRepository(cached(listOf(past1, div3)))
            newViewModel(repository).state.test {
                val state = awaitUntil { it.status == ContestsStatus.Loaded }
                assertEquals(listOf(ContestSection.Upcoming, ContestSection.Past), state.groups.map { it.section })
            }
        }

    @Test
    fun divisionFilter_keepsOnlySelectedDivisions_andTogglingAgainClearsIt() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                viewModel.toggleDivision(ContestDivision.Div2)
                val filtered = awaitUntil { it.filter.divisions == setOf(ContestDivision.Div2) }
                assertEquals(listOf(running2, unscheduled, past1), filtered.contests())

                viewModel.toggleDivision(ContestDivision.Div2)
                val cleared = awaitUntil { it.filter.isEmpty }
                assertEquals(7, cleared.contests().size)
            }
        }

    @Test
    fun kindFilter_keepsOnlySelectedKinds() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                viewModel.toggleKind(ContestKind.Icpc)
                val state = awaitUntil { it.filter.kinds == setOf(ContestKind.Icpc) }
                assertEquals(listOf(div3, educational), state.contests())
            }
        }

    @Test
    fun filters_combine_anyOfEachGroup_andBothGroups() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                viewModel.toggleDivision(ContestDivision.Div2)
                viewModel.toggleDivision(ContestDivision.Div3)
                viewModel.toggleKind(ContestKind.Icpc)
                val state = awaitUntil { it.filter.divisions.size == 2 && it.filter.kinds.isNotEmpty() }
                assertEquals(listOf(div3), state.contests())

                viewModel.clearFilters()
                assertTrue(awaitUntil { it.filter.isEmpty }.contests().isNotEmpty())
            }
        }

    @Test
    fun filterThatLeavesNothing_isEmpty() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                viewModel.toggleDivision(ContestDivision.Div4)
                val state = awaitUntil { it.filter.divisions.isNotEmpty() }
                assertEquals(ContestsStatus.Empty, state.status)
                assertTrue(state.groups.isEmpty())
            }
        }

    @Test
    fun filters_areWrittenTo_andRestoredFrom_savedState() =
        runTest {
            val handle = SavedStateHandle()
            val first = newViewModel(handle = handle)
            first.toggleDivision(ContestDivision.Div2)
            first.toggleKind(ContestKind.Cf)

            // Process death: a new view model over the handle's saved values.
            val restored = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })
            newViewModel(handle = restored).state.test {
                val state = awaitUntil { it.status == ContestsStatus.Loaded }
                assertEquals(ContestFilter(setOf(ContestDivision.Div2), setOf(ContestKind.Cf)), state.filter)
                assertEquals(listOf(running2, unscheduled, past1), state.contests())
            }
        }

    @Test
    fun cachedData_whileRefreshing_isLoaded_withoutAnIndicator() =
        runTest {
            val repository = FakeContestRepository(cached(refreshing = true))
            newViewModel(repository).state.test {
                val state = awaitUntil { it.status == ContestsStatus.Loaded }
                assertFalse(state.userRefreshing)
                assertNull(state.error)
            }
        }

    @Test
    fun cachedData_withError_isLoaded_withTheError() =
        runTest {
            val error = JudgeError.Offline()
            val repository = FakeContestRepository(cached(error = error))
            newViewModel(repository).state.test {
                val state = awaitUntil { it.status == ContestsStatus.Loaded }
                assertEquals(error, state.error)
                assertEquals(3, state.groups.size)
            }
        }

    @Test
    fun noData_withError_isError_andRetryingIsLoading() =
        runTest {
            val error = JudgeError.Unavailable()
            val repository = FakeContestRepository(cached(data = null, error = error))
            newViewModel(repository).state.test {
                assertEquals(error, awaitUntil { it.status == ContestsStatus.Error }.error)

                repository.cached.value = cached(data = null, refreshing = true, error = error)
                awaitUntil { it.status == ContestsStatus.Loading }
            }
        }

    @Test
    fun noData_whileRefreshing_isLoading() =
        runTest {
            val repository = FakeContestRepository(cached(data = null, refreshing = true))
            newViewModel(repository).state.test {
                val state = awaitUntil { it.status == ContestsStatus.Loading }
                assertTrue(state.groups.isEmpty())
                repository.cached.value = cached()
                awaitUntil { it.status == ContestsStatus.Loaded }
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

    @Test
    fun upcomingContest_movesToRunning_whenItsStartPasses() =
        runTest {
            val soon = contest(9, "Codeforces Round 9 (Div. 2)", ContestPhase.Upcoming, Now.plusSeconds(600))
            val repository = FakeContestRepository(cached(listOf(soon)))
            newViewModel(repository, clock = virtualClock()).state.test {
                assertEquals(ContestSection.Upcoming, awaitUntil { it.groups.isNotEmpty() }.groups.single().section)

                val running = awaitUntil { it.groups.singleOrNull()?.section == ContestSection.Running }
                assertEquals(ContestPhase.Running, running.contests().single().phase)

                val past = awaitUntil { it.groups.singleOrNull()?.section == ContestSection.Past }
                assertEquals(ContestPhase.Finished, past.contests().single().phase)
            }
        }

    /** The view model on the test's main dispatcher, so its sectioning runs on virtual time. */
    private fun newViewModel(
        repository: FakeContestRepository = FakeContestRepository(cached()),
        handle: SavedStateHandle = SavedStateHandle(),
        clock: Clock = FixedClock,
    ) = ContestsViewModel(repository, handle, clock, Dispatchers.Main)

    /** A clock at [Now] plus the test's virtual time. */
    private fun TestScope.virtualClock(): Clock =
        object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId): Clock = this

            override fun instant(): Instant = Now.plusMillis(testScheduler.currentTime)
        }
}

private fun ContestsUiState.contests(): List<Contest> = groups.flatMap { it.contests }

/** Skips states until one matches [predicate]: the first item may be the initial state before the data arrives. */
private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
    while (true) {
        val state = awaitItem()
        if (predicate(state)) return state
    }
}
