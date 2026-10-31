package app.pocketcp.feature.home

import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.model.Contest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [ContestRepository] whose bounded active view is [cached]; each requested `upcomingLimit` is recorded in
 * [upcomingLimits]. The full list fails the test: Home must never hold it. Refreshes are recorded and wait for
 * [refreshGate].
 */
class FakeContestRepository(
    initial: Cached<List<Contest>>,
) : ContestRepository {
    val cached = MutableStateFlow(initial)
    val refreshes = mutableListOf<Pair<Boolean, Boolean>>()
    var refreshGate: CompletableDeferred<Unit> = CompletableDeferred(Unit)

    val upcomingLimits = mutableListOf<Int>()

    override fun contests(gym: Boolean): Flow<Cached<List<Contest>>> =
        error("Home never collects the full contest list, only activeContests")

    override fun activeContests(upcomingLimit: Int): Flow<Cached<List<Contest>>> {
        upcomingLimits += upcomingLimit
        return cached
    }

    override suspend fun refresh(
        gym: Boolean,
        force: Boolean,
    ) {
        refreshes += gym to force
        refreshGate.await()
    }
}
