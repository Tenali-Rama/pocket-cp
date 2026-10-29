package app.pocketcp.core.codeforces

import app.pocketcp.core.testing.FixtureServer
import app.pocketcp.core.testing.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeforcesApiTest {
    @Test
    fun `contest list decodes every field and sends no gym parameter`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CONTESTS)

            val contests = withApi(server) { it.contests() }

            assertEquals(listOf(2278, 2272, 2277), contests.map { it.id })
            val upcoming = contests.first()
            assertEquals("Codeforces Round 1131 (Div. 3)", upcoming.name)
            assertEquals("ICPC", upcoming.type)
            assertEquals("BEFORE", upcoming.phase)
            assertEquals(false, upcoming.frozen)
            assertEquals(8100L, upcoming.durationSeconds)
            assertEquals(1793457300L, upcoming.startTimeSeconds)
            assertEquals(-147320L, upcoming.relativeTimeSeconds)
            assertNull(upcoming.kind)
            assertEquals("CF", contests[1].type)
            assertNull(server.sentQuery("gym"))
        }

    @Test
    fun `gym list sends gym=true and decodes the gym fields`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list?gym=true", ApiFixtures.GYM)

            val gym = withApi(server) { it.contests(gym = true) }

            assertEquals("true", server.sentQuery("gym"))
            val cerc = gym.first()
            assertEquals(100299, cerc.id)
            assertEquals("MikeMirzayanov", cerc.preparedBy)
            assertEquals("http://cerc.tcs.uj.edu.pl/", cerc.websiteUrl)
            assertTrue(cerc.description.orEmpty().contains("Kraków"))
            assertEquals(4, cerc.difficulty)
            assertEquals("Official ICPC Contest", cerc.kind)
            assertEquals("Central Europe Region", cerc.icpcRegion)
            assertEquals("Poland", cerc.country)
            assertEquals("Krakow", cerc.city)
            assertEquals("2013-2014", cerc.season)
            val training = gym[1]
            assertNull(training.startTimeSeconds)
            assertNull(training.preparedBy)
            assertEquals("Training Contest", training.kind)
        }

    @Test
    fun `problemset sends the tags and decodes problems and statistics`() =
        runTest {
            val server = FixtureServer().map("/api/problemset.problems?tags=implementation;math", ApiFixtures.PROBLEMS)

            val problemset = withApi(server) { it.problemset(listOf("implementation", "math")) }

            val first = problemset.problems.first()
            assertEquals(2276, first.contestId)
            assertEquals("F", first.index)
            assertEquals("Permutation Coloring Game", first.name)
            assertEquals("PROGRAMMING", first.type)
            assertNull(first.points)
            assertEquals(2400, first.rating)
            assertEquals(listOf("brute force", "dp", "dsu", "games", "greedy", "implementation", "math"), first.tags)
            assertEquals(500.0, problemset.problems[1].points ?: 0.0, 0.0)
            val stats = problemset.problemStatistics.map { Triple(it.contestId, it.index, it.solvedCount) }
            assertEquals(listOf(Triple(2276, "F", 760), Triple(2274, "A", 7690), Triple(2273, "D", 270)), stats)
        }

    @Test
    fun `problemset without tags sends no tags parameter`() =
        runTest {
            val server = FixtureServer().map("/api/problemset.problems", ApiFixtures.PROBLEMS)

            withApi(server) { it.problemset() }

            assertNull(server.sentQuery("tags"))
        }

    @Test
    fun `user info sends the handles and decodes profiles, ignoring unknown fields`() =
        runTest {
            // "city" is in the recorded response but not in UserDto.
            assertTrue(Fixtures.text(ApiFixtures.USERS).contains("\"city\""))
            val server = FixtureServer().map("/api/user.info?handles=tourist;jiangly", ApiFixtures.USERS)

            val users = withApi(server) { it.users(listOf("tourist", "jiangly")) }

            assertEquals(listOf("tourist", "jiangly"), users.map { it.handle })
            val tourist = users.first()
            assertEquals("Gennady", tourist.firstName)
            assertEquals("Korotkevich", tourist.lastName)
            assertEquals("Belarus", tourist.country)
            assertEquals("ITMO University", tourist.organization)
            assertEquals(111, tourist.contribution)
            assertEquals("legendary grandmaster", tourist.rank)
            assertEquals(3382, tourist.rating)
            assertEquals("tourist", tourist.maxRank)
            assertEquals(4009, tourist.maxRating)
            assertEquals(1793296487L, tourist.lastOnlineTimeSeconds)
            assertEquals(1265987288L, tourist.registrationTimeSeconds)
            assertEquals(91569, tourist.friendOfCount)
            assertEquals("https://userpic.codeforces.org/422/avatar/2b5dbe87f0d859a2.jpg", tourist.avatar)
            assertEquals("https://userpic.codeforces.org/422/title/50a270ed4a722867.jpg", tourist.titlePhoto)
        }

    @Test
    fun `a FAILED envelope with HTTP 400 becomes an Api error with the comment`() =
        runTest {
            val server = FixtureServer().map("/api/user.info?handles=no_such_handle_zzzq", ApiFixtures.HANDLE_NOT_FOUND, code = 400)

            val error = withApi(server) { api -> failureOf { api.users(listOf("no_such_handle_zzzq")) } }

            assertTrue("got $error", error is CodeforcesError.Api)
            assertEquals("handles: User with handle no_such_handle_zzzq not found", (error as CodeforcesError.Api).comment)
            assertEquals(1, server.requests.size)
        }

    @Test
    fun `call limit exceeded is retried with the queue backoff, then becomes RateLimited`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CALL_LIMIT)

            val error = withApi(server) { api -> failureOf { api.contests() } }

            assertTrue("got $error", error is CodeforcesError.RateLimited)
            // The first attempt plus one retry per backoff step (2, 4, 8 s).
            assertEquals(4, server.requests.size)
        }

    @Test
    fun `a server that stays down becomes Unavailable, not RateLimited`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CONTESTS, code = 503)

            val error = withApi(server) { api -> failureOf { api.contests() } }

            assertEquals(503, (error as? CodeforcesError.Unavailable)?.code)
        }

    @Test
    fun `a body of the wrong shape becomes Malformed`() =
        runTest {
            val server = FixtureServer().map("/api/user.info?handles=tourist", ApiFixtures.CONTESTS)

            val error = withApi(server) { api -> failureOf { api.users(listOf("tourist")) } }

            assertTrue("got $error", error is CodeforcesError.Malformed)
        }

    @Test
    fun `an unexpected status without a FAILED envelope becomes Http`() =
        runTest {
            val server = FixtureServer().map("/api/contest.list", ApiFixtures.CONTESTS, code = 403)

            val error = withApi(server) { api -> failureOf { api.contests() } }

            assertEquals(403, (error as? CodeforcesError.Http)?.code)
        }

    /** The [name] query parameter of the only request [this] received. */
    private fun FixtureServer.sentQuery(name: String): String? = requests.single().url.queryParameter(name)
}
