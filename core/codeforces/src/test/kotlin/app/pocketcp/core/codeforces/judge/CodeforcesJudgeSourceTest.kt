package app.pocketcp.core.codeforces.judge

import app.pocketcp.core.codeforces.ApiFixtures
import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.CodeforcesPages
import app.pocketcp.core.codeforces.failureOf
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.ContestId
import app.pocketcp.core.model.ContestKind
import app.pocketcp.core.model.ContestPhase
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.JudgeId
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.model.User
import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.network.RequestQueue
import app.pocketcp.core.parser.StatementParseError
import app.pocketcp.core.testing.FixtureServer
import app.pocketcp.core.testing.Fixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant

class CodeforcesJudgeSourceTest {
    @Test
    fun `the judge id is codeforces`() =
        runTest {
            withSource(FixtureServer()) { assertEquals(JudgeId("codeforces"), it.judge) }
        }

    @Test
    fun `contest phases map to the domain, and an unknown phase to Unknown`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", PHASES)

            val contests = withSource(server) { it.contests(gym = false) }

            val expected =
                listOf(
                    2282 to ContestPhase.Upcoming,
                    2272 to ContestPhase.Running,
                    2277 to ContestPhase.Running,
                    2261 to ContestPhase.Running,
                    2276 to ContestPhase.Finished,
                    2273 to ContestPhase.Unknown,
                )
            assertEquals(expected, contests.map { it.id.value to it.phase })
            assertTrue(contests.none { it.isGym })
        }

    @Test
    fun `start and duration are converted from seconds`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CONTESTS)

            val upcoming = withSource(server) { it.contests(gym = false) }.first()

            val expected =
                Contest(
                    id = ContestId(2278),
                    name = "Codeforces Round 1131 (Div. 3)",
                    phase = ContestPhase.Upcoming,
                    start = Instant.parse("2026-10-31T14:35:00Z"),
                    duration = Duration.ofMinutes(135),
                    isGym = false,
                    kind = ContestKind.Icpc,
                )
            assertEquals(expected, upcoming)
        }

    @Test
    fun `contest types map to kinds`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CONTESTS)

            val contests = withSource(server) { it.contests(gym = false) }

            val expected =
                listOf(2278 to ContestKind.Icpc, 2272 to ContestKind.Cf, 2277 to ContestKind.Cf, 2274 to ContestKind.Cf)
            assertEquals(expected, contests.map { it.id.value to it.kind })
        }

    @Test
    fun `IOI maps to Ioi, and an unknown type to Unknown`() {
        // The recorded contest.list has no IOI round.
        assertEquals(ContestKind.Ioi, contestKind("IOI"))
        assertEquals(ContestKind.Unknown, contestKind("TEAM"))
    }

    @Test
    fun `gym=true marks every contest as a gym and keeps a missing start null`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list?gym=true", ApiFixtures.GYM)

            val gyms = withSource(server) { it.contests(gym = true) }

            assertEquals(listOf(true, true), gyms.map { it.isGym })
            assertEquals(Instant.ofEpochSecond(1507293000), gyms[0].start)
            assertEquals(Duration.ofHours(5), gyms[0].duration)
            assertNull(gyms[1].start)
            assertEquals(Duration.ofHours(2), gyms[1].duration)
        }

    @Test
    fun `problemset joins the statistics as solvedCount`() =
        runTest {
            val server = FixtureServer().map("/api/problemset.problems", ApiFixtures.PROBLEMS)

            val problems = withSource(server) { it.problemset() }

            val first =
                Problem(
                    id = ProblemId(2276, "F"),
                    name = "Permutation Coloring Game",
                    rating = 2400,
                    tags = listOf("brute force", "dp", "dsu", "games", "greedy", "implementation", "math"),
                    solvedCount = 760,
                )
            assertEquals(first, problems.first())
            assertEquals(listOf(760, 7690, 270), problems.map { it.solvedCount })
        }

    @Test
    fun `a problem without rating or statistics maps to null, never 0`() =
        runTest {
            val server = FixtureServer().map("/api/problemset.problems", MIXED)

            val byId = withSource(server) { it.problemset() }.associateBy { it.id }

            val unrated = byId.getValue(ProblemId(2277, "A"))
            assertNull(unrated.rating)
            assertEquals(10475, unrated.solvedCount)
            val noStatistics = byId.getValue(ProblemId(2276, "C"))
            assertEquals(1100, noStatistics.rating)
            assertNull(noStatistics.solvedCount)
            assertEquals(344, byId.getValue(ProblemId(2276, "G")).solvedCount)
        }

    @Test
    fun `a problem without a contestId is dropped`() =
        runTest {
            // The fixture's acmsguru problem has no contestId.
            val fixture = Fixtures.text(MIXED)
            assertTrue(fixture.contains("\"Sultan's Pearls\""))
            val inFixture = Regex("\"type\": \"PROGRAMMING\"").findAll(fixture).count()
            val server = FixtureServer().map("/api/problemset.problems", MIXED)

            val problems = withSource(server) { it.problemset() }

            assertEquals(inFixture - 1, problems.size)
            assertTrue(problems.none { it.name == "Sultan's Pearls" })
        }

    @Test
    fun `an unrated user has null ratings`() =
        runTest {
            val server = FixtureServer().map("/api/user.info?handles=codeforces;tourist", UNRATED)

            val users = withSource(server) { it.users(listOf("codeforces", "tourist")) }

            val expected =
                listOf(
                    User(
                        handle = "codeforces",
                        rating = null,
                        maxRating = null,
                        avatarUrl = "https://userpic.codeforces.org/no-avatar.jpg",
                    ),
                    User(
                        handle = "tourist",
                        rating = 3382,
                        maxRating = 4009,
                        avatarUrl = "https://userpic.codeforces.org/422/avatar/2b5dbe87f0d859a2.jpg",
                    ),
                )
            assertEquals(expected, users)
        }

    @Test
    fun `a statement parses its title and samples`() =
        runTest {
            val server = FixtureServer().map("/contest/2274/problem/A?locale=en", PAGE_EN)

            val statement = withSource(server) { it.statement(ProblemId(2274, "A"), StatementLanguage.En) }

            assertEquals("The Projections of Paco", statement.title)
            assertEquals(2, statement.samples.size)
        }

    @Test
    fun `a challenge page and a non-problem page are malformed`() =
        runTest {
            val server =
                FixtureServer()
                    .map("/contest/2274/problem/A?locale=en", CHALLENGE)
                    .map("/contest/2274/problem/A?locale=ru", NON_PROBLEM)

            val challenge =
                withSource(server) { failureOf { it.statement(ProblemId(2274, "A"), StatementLanguage.En) } }
            val nonProblem =
                withSource(server) { failureOf { it.statement(ProblemId(2274, "A"), StatementLanguage.Ru) } }

            assertTrue(challenge is JudgeError.Malformed)
            assertTrue((challenge as JudgeError.Malformed).cause is StatementParseError.Blocked)
            assertTrue(nonProblem is JudgeError.Malformed)
            assertTrue((nonProblem as JudgeError.Malformed).cause is StatementParseError.UnknownMarkup)
        }

    @Test
    fun `offline statement fetch maps to JudgeError Offline`() =
        runTest {
            val error =
                withSource(HttpTransport { throw IOException("offline") }) {
                    failureOf { it.statement(ProblemId(2274, "A"), StatementLanguage.En) }
                }

            assertTrue("got $error", error is JudgeError.Offline)
        }

    @Test
    fun `HTTP 503 statement fetch maps to JudgeError Unavailable`() =
        runTest {
            val server = FixtureServer().map("/contest/2274/problem/A?locale=en", PAGE_EN, code = 503)

            val error =
                withSource(server) {
                    failureOf { it.statement(ProblemId(2274, "A"), StatementLanguage.En) }
                }

            assertTrue("got $error", error is JudgeError.Unavailable)
            assertEquals(4, server.requests.size)
        }

    private companion object {
        const val PHASES = "codeforces/api/contest.list.phases.json"
        const val MIXED = "codeforces/api/problemset.problems.mixed.json"
        const val UNRATED = "codeforces/api/user.info.unrated.json"
        const val PAGE_EN = "codeforces/pages/contest/2274/problem/A.en.html"
        const val CHALLENGE = "codeforces/pages/error.cloudflare.html"
        const val NON_PROBLEM = "codeforces/pages/error.non-problem.html"
    }
}

/** Runs [block] against a [CodeforcesJudgeSource] on the test [withApi] harness. */
internal suspend fun <T> TestScope.withSource(
    transport: HttpTransport,
    block: suspend TestScope.(CodeforcesJudgeSource) -> T,
): T {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    try {
        val queue = RequestQueue(transport, scope)
        return block(CodeforcesJudgeSource(CodeforcesApi(queue, scope), CodeforcesPages(queue)))
    } finally {
        scope.cancel()
    }
}
