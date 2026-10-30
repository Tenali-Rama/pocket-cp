package app.pocketcp.feature.problemset

import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ProblemRepository
import app.pocketcp.core.model.Problem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [ProblemRepository] whose problemset is [cached]. Refreshes record `force` and wait for [refreshGate]. */
class FakeProblemRepository(
    initial: Cached<List<Problem>>,
) : ProblemRepository {
    val cached = MutableStateFlow(initial)
    val refreshes = mutableListOf<Boolean>()
    var refreshGate: CompletableDeferred<Unit> = CompletableDeferred(Unit)

    override fun problemset(): Flow<Cached<List<Problem>>> = cached

    override suspend fun refresh(force: Boolean) {
        refreshes += force
        refreshGate.await()
    }
}
