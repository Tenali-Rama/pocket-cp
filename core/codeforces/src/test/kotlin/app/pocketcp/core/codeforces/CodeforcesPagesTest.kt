package app.pocketcp.core.codeforces

import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.network.RequestQueue
import app.pocketcp.core.testing.FixtureServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CodeforcesPagesTest {
    @Test
    fun `problem requests use contest or gym paths and the selected locale`() =
        runTest {
            val server =
                FixtureServer()
                    .map("/contest/2274/problem/A?locale=en", PAGE_EN)
                    .map("/contest/2274/problem/A?locale=ru", PAGE_RU)
                    .map("/gym/100299/problem/A?locale=ru", PAGE_RU)

            withPages(server) { pages ->
                pages.problem(ProblemId(2274, "A"), StatementLanguage.En)
                pages.problem(ProblemId(2274, "A"), StatementLanguage.Ru)
                pages.problem(ProblemId(100299, "A"), StatementLanguage.Ru)
            }

            assertEquals(
                listOf(
                    "/contest/2274/problem/A?locale=en",
                    "/contest/2274/problem/A?locale=ru",
                    "/gym/100299/problem/A?locale=ru",
                ),
                server.requests.map { it.url.encodedPath + "?" + it.url.query },
            )
        }

    @Test
    fun `problemUrl builds the same URL without network access`() {
        val scope = CoroutineScope(SupervisorJob())
        try {
            val pages = CodeforcesPages(RequestQueue(HttpTransport { error("not called") }, scope))
            assertEquals(
                "https://codeforces.com/contest/2274/problem/A?locale=en",
                pages.problemUrl(ProblemId(2274, "A"), StatementLanguage.En),
            )
            assertEquals(
                "https://codeforces.com/gym/100299/problem/A?locale=ru",
                pages.problemUrl(ProblemId(100299, "A"), StatementLanguage.Ru),
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `offline is mapped to CodeforcesError Offline`() =
        runTest {
            val transport = HttpTransport { throw IOException("offline") }

            val error = withPages(transport) { pages -> failureOf { pages.problem(PROBLEM, EN) } }

            assertTrue("got $error", error is CodeforcesError.Offline)
        }

    @Test
    fun `HTTP 503 is mapped to CodeforcesError Unavailable after retries`() =
        runTest {
            val server = FixtureServer().map("/contest/2274/problem/A?locale=en", PAGE_EN, code = 503)

            val error = withPages(server) { pages -> failureOf { pages.problem(PROBLEM, EN) } }

            assertEquals(503, (error as? CodeforcesError.Unavailable)?.code)
            assertEquals(4, server.requests.size)
        }

    private suspend fun <T> TestScope.withPages(
        transport: HttpTransport,
        block: suspend (CodeforcesPages) -> T,
    ): T {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            return block(CodeforcesPages(RequestQueue(transport, scope)))
        } finally {
            scope.cancel()
        }
    }

    private companion object {
        val PROBLEM = ProblemId(2274, "A")
        val EN = StatementLanguage.En
        const val PAGE_EN = "codeforces/pages/contest/2274/problem/A.en.html"
        const val PAGE_RU = "codeforces/pages/contest/2274/problem/A.ru.html"
    }
}
