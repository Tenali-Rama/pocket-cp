package app.pocketcp.feature.contests

import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.judge.CodeforcesJudgeSource
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.model.Problem
import app.pocketcp.core.network.RequestQueue
import app.pocketcp.core.testing.FixtureServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * When `contest.list.json` was recorded (`startTimeSeconds + relativeTimeSeconds` of its contests): the screenshots'
 * now, as in debug builds (ADR 0015), so the upcoming contest shows its recorded countdown.
 */
internal val FixtureNow: Instant = Instant.ofEpochSecond(1_793_309_980)

/** Stopped at [FixtureNow] in UTC, so countdowns and dates don't depend on when or where tests run. */
internal val FixtureClock: Clock = Clock.fixed(FixtureNow, ZoneOffset.UTC)

/**
 * `testdata/codeforces/api/contest.list.json` through the real Codeforces mapping: the upcoming Div. 3 2278, and
 * the finished Div. 2 rounds 2272, 2277 and 2274.
 */
internal val FixtureContests: List<Contest> by lazy {
    loadFixture("/api/contest.list", "codeforces/api/contest.list.json") { it.contests(gym = false) }
}

/** `testdata/codeforces/api/problemset.problems.json` through the real mapping: 2276F, 2274A and 2273D. */
internal val FixtureProblems: List<Problem> by lazy {
    loadFixture("/api/problemset.problems", "codeforces/api/problemset.problems.json") { it.problemset() }
}

private fun <T> loadFixture(
    path: String,
    fixture: String,
    call: suspend (JudgeSource) -> List<T>,
): List<T> {
    var result = emptyList<T>()
    runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val server = FixtureServer().map(path, fixture)
            result = call(CodeforcesJudgeSource(CodeforcesApi(RequestQueue(server, scope), scope)))
        } finally {
            scope.cancel()
        }
    }
    return result
}
