package app.pocketcp.feature.contests

import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.model.Contest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [ContestRepository] whose regular contests are [cached]. Refreshes are recorded and wait for [refreshGate]. */
class FakeContestRepository(
    initial: Cached<List<Contest>>,
) : ContestRepository {
    val cached = MutableStateFlow(initial)
    val refreshes = mutableListOf<Pair<Boolean, Boolean>>()
    var refreshGate: CompletableDeferred<Unit> = CompletableDeferred(Unit)

    override fun contests(gym: Boolean): Flow<Cached<List<Contest>>> {
        check(!gym) { "The Contests tab never shows gyms" }
        return cached
    }

    override fun activeContests(upcomingLimit: Int): Flow<Cached<List<Contest>>> = error("The Contests tab never uses the bounded view")

    override suspend fun refresh(
        gym: Boolean,
        force: Boolean,
    ) {
        refreshes += gym to force
        refreshGate.await()
    }
}
