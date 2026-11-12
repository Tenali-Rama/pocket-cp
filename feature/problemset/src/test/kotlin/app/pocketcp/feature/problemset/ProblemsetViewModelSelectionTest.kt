package app.pocketcp.feature.problemset

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.pocketcp.core.data.Cached
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.Instant

private val p1901b = Problem(ProblemId(1901, "B"), "Zebra Tiles", 1500, listOf("greedy"), 900)
private val p1900a = Problem(ProblemId(1900, "A"), "Cover in Water", 800, listOf("math"), 30_000)
private val p1899c = Problem(ProblemId(1899, "C"), "Bad Water", 1200, listOf("greedy"), 10)

/** In repository order: newest contest first. */
private val all = listOf(p1901b, p1900a, p1899c)

/** The opened problem of `ProblemsetViewModel` (ADR 0016): open, close, the default and the route's seed. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProblemsetViewModelSelectionTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun openAndClose_areKeptInSavedState_andSurviveProcessDeath() =
        runTest {
            val handle = SavedStateHandle()
            val viewModel = newViewModel(handle)
            viewModel.state.test {
                viewModel.open(p1900a.id)
                assertEquals(p1900a.id, awaitUntil { it.opened != null }.opened)
                assertEquals("1900A", handle.get<String>(KEY_OPENED))
            }
            newViewModel(handle.restored()).state.test {
                val state = awaitUntil { it.status == ProblemsetStatus.Loaded }
                assertEquals(p1900a.id, state.opened)
                assertEquals(p1900a.id, state.shown)
            }

            viewModel.state.test {
                viewModel.close()
                val closed = awaitUntil { it.opened == null && it.status == ProblemsetStatus.Loaded }
                // Closed: wide windows fall back to the list's first problem.
                assertEquals(p1901b.id, closed.shown)
                assertTrue(handle.contains(KEY_OPENED))
            }
        }

    @Test
    fun shownByDefault_followsTheFilteredList() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                assertEquals(p1901b.id, awaitUntil { it.status == ProblemsetStatus.Loaded }.shown)

                viewModel.search("water")
                val searched = awaitUntil { it.options.query == "water" }
                assertNull(searched.opened)
                assertEquals(p1900a.id, searched.shown)

                viewModel.search("1899")
                assertEquals(p1899c.id, awaitUntil { it.options.query == "1899" }.shown)

                viewModel.search("dijkstra")
                assertNull(awaitUntil { it.options.query == "dijkstra" }.shown)
            }
        }

    @Test
    fun openedProblem_staysShown_whenTheListNoLongerHasIt() =
        runTest {
            val viewModel = newViewModel()
            viewModel.state.test {
                viewModel.open(p1899c.id)
                viewModel.search("1901")
                val state = awaitUntil { it.options.query == "1901" }
                assertEquals(listOf(p1901b), state.problems)
                assertEquals(p1899c.id, state.shown)
            }
        }

    @Test
    fun routeId_opensOnce_andNotAfterClose() =
        runTest {
            val handle = SavedStateHandle(mapOf(ARG_ID to "1900A"))
            val viewModel = newViewModel(handle)
            // The seed shows from the very first state, before the list is computed.
            assertEquals(p1900a.id, viewModel.state.value.opened)
            viewModel.state.test {
                viewModel.close()
                awaitUntil { it.opened == null }
            }

            // Recreated with the route's argument still in the handle: the saved "closed" wins.
            newViewModel(handle.restored()).state.test {
                val state = awaitUntil { it.status == ProblemsetStatus.Loaded }
                assertNull(state.opened)
                assertEquals(p1901b.id, state.shown)
            }
        }

    @Test
    fun anIdOutsideTheCache_stillOpens() =
        runTest {
            val seeded = newViewModel(SavedStateHandle(mapOf(ARG_ID to "2274A")))
            seeded.state.test {
                val state = awaitUntil { it.status == ProblemsetStatus.Loaded }
                assertEquals(ProblemId(2274, "A"), state.opened)
                assertEquals(ProblemId(2274, "A"), state.shown)
            }

            val opened = newViewModel()
            opened.state.test {
                opened.open(ProblemId(9999, "Z"))
                assertEquals(ProblemId(9999, "Z"), awaitUntil { it.opened != null }.shown)
            }
        }

    @Test
    fun malformedRouteId_opensNothing() =
        runTest {
            newViewModel(SavedStateHandle(mapOf(ARG_ID to "latest"))).state.test {
                val state = awaitUntil { it.status == ProblemsetStatus.Loaded }
                assertNull(state.opened)
                assertEquals(p1901b.id, state.shown)
            }
        }

    @Test
    fun problemIdOf_readsDisplays() {
        assertEquals(ProblemId(2274, "A"), problemIdOf("2274A"))
        assertEquals(ProblemId(1971, "B2"), problemIdOf("1971B2"))
        assertNull(problemIdOf("2274"))
        assertNull(problemIdOf("A2274"))
        assertNull(problemIdOf(""))
        assertNull(problemIdOf(null))
    }

    /** A new handle with this one's saved values, as after process death. */
    private fun SavedStateHandle.restored() = SavedStateHandle(keys().associateWith { get<Any>(it) })

    private fun newViewModel(handle: SavedStateHandle = SavedStateHandle()) =
        ProblemsetViewModel(
            FakeProblemRepository(Cached(all, fetchedAt = Instant.EPOCH, refreshing = false, error = null)),
            FakeContestRepository(emptyMap()),
            FakeSettingsRepository(),
            handle,
            Dispatchers.Main,
        )
}
