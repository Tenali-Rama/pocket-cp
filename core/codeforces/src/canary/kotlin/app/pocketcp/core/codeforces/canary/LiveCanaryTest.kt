package app.pocketcp.core.codeforces.canary

import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Live Codeforces checks, run only by `liveCanary` (canary.yml, nightly). Each one fails with a message that names
 * the endpoint or selector that broke, or the Cloudflare block (see [CanaryReport]).
 */
class LiveCanaryTest {
    @Test
    fun `contest list decodes`() =
        runBlocking {
            val contests = live.endpoint("contest.list") { it.contests() }
            assertTrue(CanaryReport.endpointBroken("contest.list", "no contests"), contests.isNotEmpty())
        }

    @Test
    fun `problemset decodes`() =
        runBlocking {
            val problemset = live.endpoint("problemset.problems") { it.problemset() }
            assertTrue(
                CanaryReport.endpointBroken("problemset.problems", "no problems"),
                problemset.problems.isNotEmpty(),
            )
        }

    @Test
    fun `user info decodes`() =
        runBlocking {
            val users = live.endpoint("user.info") { it.users(listOf(HANDLE)) }
            assertEquals(
                CanaryReport.endpointBroken("user.info", "wrong user"),
                listOf(HANDLE),
                users.map { it.handle },
            )
        }

    @Test
    fun `stable problem statements parse in English and Russian`() =
        runBlocking {
            live.statement(ProblemId(1, "A"), StatementLanguage.En)
            live.statement(ProblemId(1, "A"), StatementLanguage.Ru)
        }

    companion object {
        private const val HANDLE = "tourist"

        @get:ClassRule
        @JvmStatic
        val tmp = TemporaryFolder()

        /** One client and one queue for every check, so the live API calls are spaced like the app's. */
        private lateinit var live: LiveCodeforces

        @BeforeClass
        @JvmStatic
        fun setUp() {
            live = LiveCodeforces(tmp.newFolder("http"))
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            live.close()
        }
    }
}
