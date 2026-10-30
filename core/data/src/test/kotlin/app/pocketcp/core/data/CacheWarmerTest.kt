package app.pocketcp.core.data

import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.Problem
import app.pocketcp.core.network.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Provider

/** [CacheWarmer] on fake repositories and a fake connection, on virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class CacheWarmerTest {
    private val online = MutableStateFlow(false)
    private val contests = FakeContestRepository()
    private val problems = FakeProblemRepository()

    @Test
    fun `starting online refreshes regular contests and the problemset once, not forced`() =
        runTest {
            online.value = true

            start()

            assertEquals(listOf(Refresh(gym = false, force = false)), contests.refreshes)
            assertEquals(listOf(false), problems.refreshes)
        }

    @Test
    fun `starting offline refreshes nothing until the connection comes up`() =
        runTest {
            start()
            assertEquals(0, contests.refreshes.size)
            assertEquals(0, problems.refreshes.size)

            online.value = true
            runCurrent()

            assertEquals(1, contests.refreshes.size)
            assertEquals(1, problems.refreshes.size)
        }

    @Test
    fun `every reconnect refreshes again`() =
        runTest {
            online.value = true
            start()

            online.value = false
            runCurrent()
            assertEquals(1, contests.refreshes.size)

            online.value = true
            runCurrent()

            assertEquals(2, contests.refreshes.size)
            assertEquals(2, problems.refreshes.size)
        }

    @Test
    fun `a failed refresh neither skips the other one nor stops later reconnects`() =
        runTest {
            contests.failure = IllegalStateException("disk full")
            online.value = true
            start()
            assertEquals(1, problems.refreshes.size)

            contests.failure = null
            online.value = false
            runCurrent()
            online.value = true
            runCurrent()

            assertEquals(2, contests.refreshes.size)
            assertEquals(2, problems.refreshes.size)
        }

    @Test
    fun `starting twice watches the connection once`() =
        runTest {
            online.value = true
            val warmer = warmer()

            warmer.start()
            warmer.start()
            runCurrent()

            assertEquals(1, contests.refreshes.size)
        }

    private fun TestScope.warmer(): CacheWarmer {
        val monitor =
            object : NetworkMonitor {
                override val isOnline: Flow<Boolean> = online
            }
        val contestRepository = Provider<ContestRepository> { contests }
        val problemRepository = Provider<ProblemRepository> { problems }
        return CacheWarmer(monitor, contestRepository, problemRepository, backgroundScope)
    }

    private fun TestScope.start() {
        warmer().start()
        runCurrent()
    }

    private data class Refresh(
        val gym: Boolean,
        val force: Boolean,
    )

    /** Records refreshes; throws [failure] (after recording) when set. */
    private class FakeContestRepository : ContestRepository {
        val refreshes = mutableListOf<Refresh>()
        var failure: Exception? = null

        override fun contests(gym: Boolean): Flow<Cached<List<Contest>>> = emptyFlow()

        override suspend fun refresh(
            gym: Boolean,
            force: Boolean,
        ) {
            refreshes += Refresh(gym, force)
            failure?.let { throw it }
        }
    }

    /** Records each refresh's `force`. */
    private class FakeProblemRepository : ProblemRepository {
        val refreshes = mutableListOf<Boolean>()

        override fun problemset(): Flow<Cached<List<Problem>>> = emptyFlow()

        override suspend fun refresh(force: Boolean) {
            refreshes += force
        }
    }
}
