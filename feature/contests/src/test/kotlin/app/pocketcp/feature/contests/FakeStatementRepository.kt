package app.pocketcp.feature.contests

import app.pocketcp.core.data.Cached
import app.pocketcp.core.data.StatementRepository
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** A [StatementRepository] that records each [prefetch] in [prefetches]; the contest detail never reads one. */
class FakeStatementRepository : StatementRepository {
    val prefetches = mutableListOf<Pair<List<ProblemId>, StatementLanguage>>()

    override fun statement(
        problem: ProblemId,
        language: StatementLanguage,
    ): Flow<Cached<Statement>> = flowOf(Cached(null, null, refreshing = false, error = null))

    override suspend fun refresh(
        problem: ProblemId,
        language: StatementLanguage,
        force: Boolean,
    ) = Unit

    override fun prefetch(
        problems: List<ProblemId>,
        language: StatementLanguage,
    ) {
        prefetches += problems to language
    }

    override fun pageUrl(
        problem: ProblemId,
        language: StatementLanguage,
    ): String = "https://codeforces.com/contest/${problem.contestId}/problem/${problem.index}"
}
