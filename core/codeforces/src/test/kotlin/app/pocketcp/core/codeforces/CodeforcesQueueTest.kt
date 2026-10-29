package app.pocketcp.core.codeforces

import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.testing.FixtureServer
import kotlinx.coroutines.async
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeforcesQueueTest {
    @Test
    fun `three different calls start 2 s apart because they share the request queue`() =
        runTest {
            val server =
                FixtureServer()
                    .map("/api/contest.list", ApiFixtures.CONTESTS)
                    .map("/api/problemset.problems", ApiFixtures.PROBLEMS)
                    .map("/api/user.info?handles=tourist;jiangly", ApiFixtures.USERS)
            val starts = mutableListOf<Pair<Long, String>>()
            val timed =
                HttpTransport { request ->
                    starts += currentTime to request.url.encodedPath
                    server.execute(request)
                }

            withApi(timed) { api ->
                val contests = async { api.contests() }
                val problems = async { api.problemset() }
                val users = async { api.users(listOf("tourist", "jiangly")) }
                contests.await()
                problems.await()
                users.await()
            }

            val expected =
                listOf(
                    0L to "/api/contest.list",
                    2_000L to "/api/problemset.problems",
                    4_000L to "/api/user.info",
                )
            assertEquals(expected, starts)
        }
}
