package app.pocketcp.core.data

import app.pocketcp.core.database.CacheKeys
import app.pocketcp.core.database.CacheMetaDao
import app.pocketcp.core.database.ContestDao
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.JudgeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration

/** Contests and gyms, cache first (ADR 0011). */
interface ContestRepository {
    /** The cached contests (or gyms), refreshed in the background when older than [MAX_AGE]. */
    fun contests(gym: Boolean): Flow<Cached<List<Contest>>>

    /**
     * The regular contests' running ones, then at most [upcomingLimit] upcoming ones; each group earliest start
     * first, no start last. A bounded view of `contests(gym = false)`: same fetch time, refresh status and refresh.
     * Throws [IllegalArgumentException] if [upcomingLimit] is negative.
     */
    fun activeContests(upcomingLimit: Int): Flow<Cached<List<Contest>>>

    /** Refreshes when stale, or always when [force]. Returns when done; failures show up in [contests]. */
    suspend fun refresh(
        gym: Boolean,
        force: Boolean = false,
    )

    companion object {
        val MAX_AGE: Duration = Duration.ofMinutes(5)
    }
}

internal class CachedContestRepository(
    source: JudgeSource,
    private val dao: ContestDao,
    meta: CacheMetaDao,
    clock: Clock,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ContestRepository {
    private val resources =
        listOf(false, true).associateWith { gym ->
            val dataset =
                object : CachedDataset<List<Contest>> {
                    override fun cached() = dao.contests(gym).map { contests -> contests.map { it.toDomain() } }

                    override suspend fun fetchedAtMillis() = meta.fetchedAt(CacheKeys.contests(gym))

                    override suspend fun fetch() {
                        // replaceAll rejects a contest from the other dataset; drop a judge's stray record instead.
                        val contests = source.contests(gym).filter { it.isGym == gym }.map { it.toEntity() }
                        dao.replaceAll(gym, contests, clock.millis())
                    }
                }
            CachedResource(dataset, ContestRepository.MAX_AGE, clock, scope, dispatcher)
        }

    override fun contests(gym: Boolean): Flow<Cached<List<Contest>>> = resources.getValue(gym).stream()

    override fun activeContests(upcomingLimit: Int): Flow<Cached<List<Contest>>> {
        require(upcomingLimit >= 0) { "upcomingLimit must be at least 0, was $upcomingLimit" }
        val active = dao.activeContests(upcomingLimit).map { contests -> contests.map { it.toDomain() } }
        return resources.getValue(false).stream(active)
    }

    override suspend fun refresh(
        gym: Boolean,
        force: Boolean,
    ) = resources.getValue(gym).refresh(force)
}
