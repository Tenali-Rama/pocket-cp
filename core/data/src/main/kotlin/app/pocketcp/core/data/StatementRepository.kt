package app.pocketcp.core.data

import app.pocketcp.core.database.StatementDao
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/** Problem statements, cache first (ADR 0011), one cached row per problem and language. */
interface StatementRepository {
    /**
     * The cached statement, refreshed in the background when missing, older than [MAX_AGE], or stored in an older
     * format. One refresh per problem and language at a time, shared with [refresh] and [prefetch].
     */
    fun statement(
        problem: ProblemId,
        language: StatementLanguage,
    ): Flow<Cached<Statement>>

    /** Refreshes when stale, or always when [force]. Returns when done; failures show up in [statement]. */
    suspend fun refresh(
        problem: ProblemId,
        language: StatementLanguage,
        force: Boolean = false,
    )

    /**
     * Caches [problems] (e.g. a contest's) in the background, one at a time, skipping those already cached in the
     * current format. Stops at the first `JudgeError.Offline` and skips other failures. Never throws.
     */
    fun prefetch(
        problems: List<ProblemId>,
        language: StatementLanguage,
    )

    /** The statement's page on the judge, for the WebView fallback. */
    fun pageUrl(
        problem: ProblemId,
        language: StatementLanguage,
    ): String

    companion object {
        val MAX_AGE: Duration = Duration.ofDays(7)
    }
}

internal class CachedStatementRepository(
    private val source: JudgeSource,
    private val dao: StatementDao,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : StatementRepository {
    private data class Key(
        val problem: ProblemId,
        val language: StatementLanguage,
    )

    /** One resource per row, created on first use, so each key has its own status and single in-flight refresh. */
    private val resources = ConcurrentHashMap<Key, CachedResource<Statement>>()

    override fun statement(
        problem: ProblemId,
        language: StatementLanguage,
    ): Flow<Cached<Statement>> = resource(problem, language).stream()

    override suspend fun refresh(
        problem: ProblemId,
        language: StatementLanguage,
        force: Boolean,
    ) = resource(problem, language).refresh(force)

    override fun prefetch(
        problems: List<ProblemId>,
        language: StatementLanguage,
    ) {
        launchPrefetch(problems, language)
    }

    /** [prefetch], returning its job so tests can wait for it. */
    @Suppress("TooGenericExceptionCaught", "RethrowCaughtException", "SwallowedException")
    internal fun launchPrefetch(
        problems: List<ProblemId>,
        language: StatementLanguage,
    ): Job =
        scope.launch {
            try {
                for (problem in uncached(problems, language)) {
                    val error = resource(problem, language).refreshForError(force = false)
                    if (error is JudgeError.Offline) break
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ignored: Exception) {
                // E.g. an SQLiteException reading the cache: prefetch is best effort and must not crash the scope.
            }
        }

    override fun pageUrl(
        problem: ProblemId,
        language: StatementLanguage,
    ): String = source.problemUrl(problem, language)

    /** [problems] in order, without those cached in [language] in the current format. */
    private suspend fun uncached(
        problems: List<ProblemId>,
        language: StatementLanguage,
    ): List<ProblemId> {
        val cached =
            problems.map { it.contestId }.distinct().associateWith { contestId ->
                dao.cachedIndices(contestId, language.key, STATEMENT_FORMAT_VERSION).toSet()
            }
        return problems.distinct().filter { it.index !in cached.getValue(it.contestId) }
    }

    private fun resource(
        problem: ProblemId,
        language: StatementLanguage,
    ): CachedResource<Statement> =
        resources.computeIfAbsent(Key(problem, language)) {
            CachedResource(row(problem, language), StatementRepository.MAX_AGE, clock, scope, dispatcher)
        }

    /** The row of [problem] in [language]. A row of another format version, or a corrupt one, reads as missing. */
    private fun row(
        problem: ProblemId,
        language: StatementLanguage,
    ) = object : CachedDataset<Statement> {
        private fun entity() = dao.statement(problem.contestId, problem.index, language.key)

        override fun cached() = entity().map { it?.toDomain() }

        override suspend fun fetchedAtMillis(): Long? {
            val entity = entity().first() ?: return null
            return entity.fetchedAtEpochMillis.takeIf { entity.toDomain() != null }
        }

        override suspend fun fetch() = dao.upsert(source.statement(problem, language).toEntity(clock.millis()))
    }
}
