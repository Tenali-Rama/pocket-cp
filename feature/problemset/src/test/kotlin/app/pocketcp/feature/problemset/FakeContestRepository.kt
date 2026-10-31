package app.pocketcp.feature.problemset

import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.ContestRepository
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Duration

/**
 * A [ContestRepository] whose cached regular contests are finished ones named per [names] (by contest id), so each
 * has the division its name states. Refreshing does nothing.
 */
class FakeContestRepository(
    names: Map<Int, String> = emptyMap(),
) : ContestRepository {
    private val contests =
        names.map { (id, name) ->
            Contest(ContestId(id), name, ContestPhase.Finished, start = null, Duration.ofHours(2), isGym = false)
        }

    override fun contests(gym: Boolean): Flow<Cached<List<Contest>>> {
        check(!gym) { "Divisions come from regular contests only" }
        return flowOf(Cached(contests, fetchedAt = null, refreshing = false, error = null))
    }

    override suspend fun refresh(
        gym: Boolean,
        force: Boolean,
    ) = Unit
}
