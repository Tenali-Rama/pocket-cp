package app.pocketcp.feature.contests

import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.model.Problem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [ProblemRepository] whose problemset is [cached]. Each refresh's `force` is recorded in [refreshes]. */
class FakeProblemRepository(
    initial: Cached<List<Problem>>,
) : ProblemRepository {
    val cached = MutableStateFlow(initial)
    val refreshes = mutableListOf<Boolean>()

    override fun problemset(): Flow<Cached<List<Problem>>> = cached

    override suspend fun refresh(force: Boolean) {
        refreshes += force
    }
}
