package app.pocketcp.core.data

import app.pocketcp.core.database.CacheKeys
import app.pocketcp.core.database.CacheMetaDao
import app.pocketcp.core.database.ProblemDao
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.model.Problem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration

/** The problemset, cache first (ADR 0010). */
interface ProblemRepository {
    /** The cached problemset, refreshed in the background when older than [MAX_AGE]. */
    fun problemset(): Flow<Cached<List<Problem>>>

    /** Refreshes when stale, or always when [force]. Returns when done; failures show up in [problemset]. */
    suspend fun refresh(force: Boolean = false)

    companion object {
        val MAX_AGE: Duration = Duration.ofHours(1)
    }
}

internal class CachedProblemRepository(
    source: JudgeSource,
    dao: ProblemDao,
    meta: CacheMetaDao,
    clock: Clock,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ProblemRepository {
    private val resource =
        CachedResource(
            scope = scope,
            dispatcher = dispatcher,
            clock = clock,
            maxAge = ProblemRepository.MAX_AGE,
            cached = { dao.problems().map { problems -> problems.map { it.toDomain() } } },
            fetchedAtMillis = { meta.fetchedAt(CacheKeys.PROBLEMSET) },
            fetch = { dao.replaceAll(source.problemset().map { it.toEntity() }, clock.millis()) },
        )

    override fun problemset(): Flow<Cached<List<Problem>>> = resource.stream()

    override suspend fun refresh(force: Boolean) = resource.refresh(force)
}
