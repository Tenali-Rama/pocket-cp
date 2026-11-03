package app.pocketcp.feature.problemset

import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.CodeforcesPages
import app.pocketcp.core.codeforces.judge.CodeforcesJudgeSource
import app.pocketcp.core.model.Problem
import app.pocketcp.core.network.RequestQueue
import app.pocketcp.core.testing.FixtureServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * `testdata/codeforces/api/problemset.problems.json` through the real Codeforces mapping, in the repository's
 * order (newest contest first, then by index): 2276F (2400), 2274A (800), 2273D (2600).
 */
internal val FixtureProblems: List<Problem> by lazy { loadFixtureProblems() }

private fun loadFixtureProblems(): List<Problem> {
    var problems = emptyList<Problem>()
    runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val server = FixtureServer().map("/api/problemset.problems", "codeforces/api/problemset.problems.json")
            val queue = RequestQueue(server, scope)
            problems = CodeforcesJudgeSource(CodeforcesApi(queue, scope), CodeforcesPages(queue)).problemset()
        } finally {
            scope.cancel()
        }
    }
    return problems.sortedWith(compareByDescending<Problem> { it.id.contestId }.thenBy { it.id.index })
}
