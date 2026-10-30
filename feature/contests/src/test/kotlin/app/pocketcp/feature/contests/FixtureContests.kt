package app.pocketcp.feature.contests

import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.judge.CodeforcesJudgeSource
import app.pocketcp.core.model.Contest
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

/** `testdata/codeforces/api/contest.list.json` through the real Codeforces mapping: one Div. 3, two Div. 2. */
internal val FixtureContests: List<Contest> by lazy { loadFixtureContests() }

private fun loadFixtureContests(): List<Contest> {
    var contests = emptyList<Contest>()
    runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val server = FixtureServer().map("/api/contest.list", "codeforces/api/contest.list.json")
            contests = CodeforcesJudgeSource(CodeforcesApi(RequestQueue(server, scope), scope)).contests(gym = false)
        } finally {
            scope.cancel()
        }
    }
    return contests
}
