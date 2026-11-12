package app.pocketcp.core.problemview

import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.StatementRepository
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart

/**
 * A [StatementRepository] holding one [Cached] statement per problem and language. Like the real repository, a
 * collection of a row without data starts a fetch: it is recorded in [fetches] and the row shows `refreshing` until
 * the test sets it. Each [refresh] is recorded in [refreshes] with its `force`.
 */
class FakeStatementRepository : StatementRepository {
    data class Key(
        val problem: ProblemId,
        val language: StatementLanguage,
    )

    private val rows = mutableMapOf<Key, MutableStateFlow<Cached<Statement>>>()
    val fetches = mutableListOf<Key>()
    val refreshes = mutableListOf<Pair<Key, Boolean>>()

    fun row(
        problem: ProblemId,
        language: StatementLanguage,
    ): MutableStateFlow<Cached<Statement>> =
        rows.getOrPut(Key(problem, language)) { MutableStateFlow(Cached(null, null, refreshing = false, error = null)) }

    override fun statement(
        problem: ProblemId,
        language: StatementLanguage,
    ): Flow<Cached<Statement>> {
        val row = row(problem, language)
        return row.onStart {
            if (row.value.data == null) {
                fetches += Key(problem, language)
                row.value = row.value.copy(refreshing = true)
            }
        }
    }

    override suspend fun refresh(
        problem: ProblemId,
        language: StatementLanguage,
        force: Boolean,
    ) {
        refreshes += Key(problem, language) to force
    }

    override fun prefetch(
        problems: List<ProblemId>,
        language: StatementLanguage,
    ) = error("The problem view never prefetches")

    override fun pageUrl(
        problem: ProblemId,
        language: StatementLanguage,
    ): String = "https://codeforces.com/contest/${problem.contestId}/problem/${problem.index}?locale=${language.key}"
}
