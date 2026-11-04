package app.pocketcp.core.codeforces.judge

import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.CodeforcesError
import app.pocketcp.core.codeforces.CodeforcesPages
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.JudgeId
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.User
import app.pocketcp.core.parser.CodeforcesStatementParser
import app.pocketcp.core.parser.StatementParseError

/** Codeforces as a [JudgeSource]: [api]'s DTOs become domain types and every [CodeforcesError] a [JudgeError]. */
class CodeforcesJudgeSource(
    private val api: CodeforcesApi,
    private val pages: CodeforcesPages,
) : JudgeSource {
    override val judge: JudgeId = ID

    override suspend fun contests(gym: Boolean): List<Contest> = call { api.contests(gym).map { it.toContest(gym) } }

    override suspend fun problemset(): List<Problem> = call { api.problemset().toProblems() }

    override suspend fun users(handles: List<String>): List<User> = call { api.users(handles).map { it.toUser() } }

    override suspend fun statement(
        problem: ProblemId,
        language: StatementLanguage,
    ): Statement =
        call {
            try {
                CodeforcesStatementParser.parse(pages.problem(problem, language), problem, language)
            } catch (error: StatementParseError) {
                throw JudgeError.Malformed(error)
            }
        }

    override fun problemUrl(
        problem: ProblemId,
        language: StatementLanguage,
    ): String = pages.problemUrl(problem, language)

    private suspend fun <T> call(block: suspend () -> T): T =
        try {
            block()
        } catch (e: CodeforcesError) {
            throw e.toJudgeError()
        }

    companion object {
        /** Persisted with cached data: never change it. */
        val ID: JudgeId = JudgeId("codeforces")
    }
}
