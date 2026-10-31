package app.pocketcp.judge

import app.pocketcp.core.codeforces.CodeforcesApi
import app.pocketcp.core.codeforces.CodeforcesError
import app.pocketcp.core.codeforces.judge.CodeforcesJudgeSource
import app.pocketcp.core.model.Contest
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.JudgeId
import app.pocketcp.core.model.JudgeSource
import app.pocketcp.core.model.Problem
import app.pocketcp.core.model.User
import app.pocketcp.core.network.RequestQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * The [JudgeSource] of debug and the benchmark build types (ADR 0015): the real [CodeforcesJudgeSource] mapping over
 * recorded responses read from [assets], so UI flows and benchmarks get stable, real-looking data and never reach
 * the network.
 * Contest starts are shifted so the moment `contest.list.json` was recorded is [clock]'s now: upcoming contests
 * keep their recorded countdowns, and phases stay as recorded.
 * @param scope where queued calls run, as for [CodeforcesApi]; [dispatcher] reads the files.
 */
class FixtureJudgeSource(
    assets: FixtureAssets,
    private val clock: Clock,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
) : JudgeSource {
    // Not under /api/, so the queue doesn't space these local reads like Codeforces calls.
    private val api = CodeforcesApi(RequestQueue(AssetTransport(assets, dispatcher), scope), scope, FIXTURE_URL)
    private val codeforces = CodeforcesJudgeSource(api)

    @Volatile private var recordedAt: Instant? = null

    override val judge: JudgeId = codeforces.judge

    override suspend fun contests(gym: Boolean): List<Contest> {
        val shift = Duration.between(recordedAt(), clock.instant())
        return codeforces.contests(gym).map { contest -> contest.copy(start = contest.start?.plus(shift)) }
    }

    override suspend fun problemset(): List<Problem> = codeforces.problemset()

    override suspend fun users(handles: List<String>): List<User> = codeforces.users(handles)

    /** When `contest.list.json` was recorded: `startTimeSeconds + relativeTimeSeconds` of any of its contests. */
    private suspend fun recordedAt(): Instant =
        recordedAt ?: run {
            val contests =
                try {
                    api.contests()
                } catch (e: CodeforcesError) {
                    throw JudgeError.Malformed(e)
                }
            val seconds =
                contests.firstNotNullOfOrNull { contest ->
                    contest.relativeTimeSeconds?.let { relative -> contest.startTimeSeconds?.plus(relative) }
                } ?: throw JudgeError.Malformed()
            Instant.ofEpochSecond(seconds).also { recordedAt = it }
        }

    private companion object {
        /** Never resolved: [AssetTransport] answers every call. */
        val FIXTURE_URL = "https://fixtures.invalid/".toHttpUrl()
    }
}
