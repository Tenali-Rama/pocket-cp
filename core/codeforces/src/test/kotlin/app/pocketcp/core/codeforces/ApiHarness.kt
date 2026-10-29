package app.pocketcp.core.codeforces

import app.pocketcp.core.network.HttpTransport
import app.pocketcp.core.network.RequestQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/** Fixture paths under `testdata/`. */
object ApiFixtures {
    const val CONTESTS = "codeforces/api/contest.list.json"
    const val GYM = "codeforces/api/contest.list.gym.json"
    const val PROBLEMS = "codeforces/api/problemset.problems.json"
    const val USERS = "codeforces/api/user.info.json"
    const val CALL_LIMIT = "codeforces/api/error.call-limit.json"
    const val HANDLE_NOT_FOUND = "codeforces/api/error.handle-not-found.json"
}

/**
 * Runs [block] against a [CodeforcesApi] on a real [RequestQueue] whose upstream is [transport]. The queue and
 * the call scope live on the test scheduler (virtual time) and are cancelled afterwards.
 */
suspend fun <T> TestScope.withApi(
    transport: HttpTransport,
    block: suspend TestScope.(CodeforcesApi) -> T,
): T {
    val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
    try {
        return block(CodeforcesApi(RequestQueue(transport, scope), scope))
    } finally {
        scope.cancel()
    }
}

/** The failure of [block], or null when it succeeds. */
suspend fun failureOf(block: suspend () -> Unit): Throwable? = runCatching { block() }.exceptionOrNull()
