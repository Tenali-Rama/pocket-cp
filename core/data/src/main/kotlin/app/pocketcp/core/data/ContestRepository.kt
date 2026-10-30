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

/** Contests and gyms, cache first (ADR 0010). */
interface ContestRepository {
    /** The cached contests (or gyms), refreshed in the background when older than [MAX_AGE]. */
    fun contests(gym: Boolean): Flow<Cached<List<Contest>>>

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
    dao: ContestDao,
    meta: CacheMetaDao,
    clock: Clock,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ContestRepository {
    private val resources =
        listOf(false, true).associateWith { gym ->
            CachedResource(
                scope = scope,
                dispatcher = dispatcher,
                clock = clock,
                maxAge = ContestRepository.MAX_AGE,
                cached = { dao.contests(gym).map { contests -> contests.map { it.toDomain() } } },
                fetchedAtMillis = { meta.fetchedAt(CacheKeys.contests(gym)) },
                fetch = {
                    // replaceAll rejects a contest from the other dataset; a judge's stray record is dropped instead.
                    val contests = source.contests(gym).filter { it.isGym == gym }.map { it.toEntity() }
                    dao.replaceAll(gym, contests, clock.millis())
                },
            )
        }

    override fun contests(gym: Boolean): Flow<Cached<List<Contest>>> = resources.getValue(gym).stream()

    override suspend fun refresh(
        gym: Boolean,
        force: Boolean,
    ) = resources.getValue(gym).refresh(force)
}
