package app.pocketcp.core.testing

import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureServerTest {
    @Test
    fun `a mapped path returns the fixture`() =
        runTest {
            val server = FixtureServer().map("/api/user.info?handles=tourist;jiangly", USER_INFO)

            val response = server.execute(get("https://codeforces.com/api/user.info?handles=tourist%3Bjiangly"))

            assertEquals(200, response.code)
            assertEquals(Fixtures.text(USER_INFO), response.body)
            assertEquals("application/json", response.headers["Content-Type"])
            assertEquals(1, server.requests.size)
        }

    @Test
    fun `a mapped path can answer with an error status`() =
        runTest {
            val server = FixtureServer().map("/api/user.info?handles=x", NOT_FOUND, code = 400)

            val response = server.execute(get("https://codeforces.com/api/user.info?handles=x"))

            assertEquals(400, response.code)
            assertEquals(Fixtures.text(NOT_FOUND), response.body)
        }

    @Test
    fun `an unmapped path fails the test with the path in the message`() =
        runTest {
            val server = FixtureServer().map("/api/user.info?handles=tourist", USER_INFO)

            val error = runCatching { server.execute(get("https://codeforces.com/api/contest.list?gym=true")) }.exceptionOrNull()

            assertTrue("got $error", error is AssertionError)
            assertTrue("got $error", error?.message.orEmpty().contains("/api/contest.list?gym=true"))
        }

    @Test
    fun `fixtures are read from the repo-root testdata directory`() {
        assertEquals("testdata", Fixtures.root.name)
        assertTrue(Fixtures.text(USER_INFO).contains("\"status\": \"OK\""))
    }

    @Test(expected = IllegalStateException::class)
    fun `a missing fixture file is reported`() {
        Fixtures.text("codeforces/api/no-such-fixture.json")
    }

    private fun get(url: String) = Request.Builder().url(url).build()

    private companion object {
        const val USER_INFO = "codeforces/api/user.info.json"
        const val NOT_FOUND = "codeforces/api/error.handle-not-found.json"
    }
}
