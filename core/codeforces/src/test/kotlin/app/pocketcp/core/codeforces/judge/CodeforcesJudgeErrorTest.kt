package app.pocketcp.core.codeforces.judge

import app.pocketcp.core.codeforces.ApiFixtures
import app.pocketcp.core.codeforces.CodeforcesError
import app.pocketcp.core.codeforces.failureOf
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.testing.FixtureServer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** One case per [CodeforcesError], each produced by a real call through the queue. */
class CodeforcesJudgeErrorTest {
    @Test
    fun `Offline becomes Offline`() =
        runTest {
            val offline = HttpTransport { throw IOException("no route to host") }

            val error = withSource(offline) { source -> failureOf { source.contests(gym = false) } }

            assertMapped<JudgeError.Offline, CodeforcesError.Offline>(error)
        }

    @Test
    fun `RateLimited becomes RateLimited`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CALL_LIMIT)

            val error = withSource(server) { source -> failureOf { source.contests(gym = false) } }

            assertMapped<JudgeError.RateLimited, CodeforcesError.RateLimited>(error)
        }

    @Test
    fun `Unavailable becomes Unavailable`() =
        runTest {
            val server = FixtureServer().map("/api/problemset.problems", ApiFixtures.PROBLEMS, code = 503)

            val error = withSource(server) { source -> failureOf { source.problemset() } }

            assertMapped<JudgeError.Unavailable, CodeforcesError.Unavailable>(error)
        }

    @Test
    fun `Api becomes Rejected with the comment as its message`() =
        runTest {
            val path = "/api/user.info?handles=no_such_handle_zzzq"
            val server = FixtureServer().map(path, ApiFixtures.HANDLE_NOT_FOUND, code = 400)

            val error = withSource(server) { source -> failureOf { source.users(listOf("no_such_handle_zzzq")) } }

            assertMapped<JudgeError.Rejected, CodeforcesError.Api>(error)
            assertEquals("handles: User with handle no_such_handle_zzzq not found", error?.message)
        }

    @Test
    fun `Http becomes Malformed`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CONTESTS, code = 403)

            val error = withSource(server) { source -> failureOf { source.contests(gym = false) } }

            assertMapped<JudgeError.Malformed, CodeforcesError.Http>(error)
        }

    @Test
    fun `Malformed becomes Malformed`() =
        runTest {
            val server = FixtureServer().map("/api/user.info?handles=tourist", ApiFixtures.CONTESTS)

            val error = withSource(server) { source -> failureOf { source.users(listOf("tourist")) } }

            assertMapped<JudgeError.Malformed, CodeforcesError.Malformed>(error)
        }

    /** [error] is a [J] caused by a [C], so the Codeforces detail stays available for logs. */
    private inline fun <reified J : JudgeError, reified C : CodeforcesError> assertMapped(error: Throwable?) {
        assertTrue("got $error", error is J)
        assertTrue("caused by ${error?.cause}", error?.cause is C)
    }
}
