package app.pocketcp.core.data

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import app.pocketcp.core.database.PocketCpDatabase
import app.pocketcp.core.database.StatementEntity
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.ProblemId
import app.pocketcp.core.model.Statement
import app.pocketcp.core.model.StatementLanguage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/** The statement cache on in-memory Room, a fake judge gated by the test, and virtual time as the clock. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StatementRepositoryTest {
    private lateinit var database: PocketCpDatabase
    private val source = FakeJudgeSource()

    @Before
    fun open() {
        database = inMemoryDatabase()
    }

    @After
    fun close() = database.close()

    @Test
    fun `a missing statement is emitted as refreshing, then the fetched one follows`() =
        runTest {
            val response = CompletableDeferred<Statement>()
            source.onStatement = { _, _ -> response.await() }

            statements().statement(a, en).test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached<Statement>(null, null, refreshing = true, error = null), awaitItem())

                response.complete(statementA)

                val fetched = awaitMatching { it.data == statementA && !it.refreshing }
                assertEquals(Cached(statementA, Instant.ofEpochMilli(currentTime), false, null), fetched)
            }
            assertEquals(1, source.statementCalls)
        }

    @Test
    fun `a fresh cached statement is emitted first, without calling the judge`() =
        runTest {
            cache(statementA)
            advanceTimeBy(6.days)

            statements().statement(a, en).test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(statementA, Instant.EPOCH, refreshing = false, error = null), awaitItem())
                runCurrent()
            }
            assertEquals(0, source.statementCalls)
        }

    @Test
    fun `a stale statement is emitted as refreshing, then replaced`() =
        runTest {
            cache(statementA)
            advanceTimeBy(StatementRepository.MAX_AGE.toMillis().milliseconds + 1.milliseconds)
            val updated = statementA.copy(title = "A. Updated")
            val response = CompletableDeferred<Statement>()
            source.onStatement = { _, _ -> response.await() }

            statements().statement(a, en).test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached(statementA, Instant.EPOCH, refreshing = true, error = null), awaitItem())

                response.complete(updated)

                val refreshed = awaitMatching { it.data == updated && !it.refreshing }
                assertEquals(Instant.ofEpochMilli(currentTime), refreshed.fetchedAt)
            }
        }

    @Test
    fun `a failed refresh keeps the cached statement and sets the error`() =
        runTest {
            cache(statementA)
            advanceTimeBy(8.days)
            val offline = JudgeError.Offline()
            source.onStatement = { _, _ -> throw offline }

            statements().statement(a, en).test(timeout = FLOW_TIMEOUT) {
                val failed = awaitMatching { !it.refreshing }
                assertEquals(Cached(statementA, Instant.EPOCH, refreshing = false, error = offline), failed)
            }
            assertEquals(statementA, stored(a)?.toDomain())
        }

    @Test
    fun `a statement stored in another format version is treated as missing and refetched`() =
        runTest {
            database.statementDao().upsert(statementA.toEntity(currentTime).copy(formatVersion = 0))
            source.onStatement = { _, _ -> statementA }

            statements().statement(a, en).test(timeout = FLOW_TIMEOUT) {
                assertEquals(Cached<Statement>(null, null, refreshing = true, error = null), awaitItem())
                awaitMatching { it.data == statementA && !it.refreshing }
            }
            assertEquals(1, source.statementCalls)
            val row = stored(a)
            assertEquals(STATEMENT_FORMAT_VERSION, row?.formatVersion)
        }

    @Test
    fun `two collectors of the same statement cause one fetch`() =
        runTest {
            val repository = statements()
            val response = CompletableDeferred<Statement>()
            source.onStatement = { _, _ -> response.await() }

            turbineScope {
                val first = repository.statement(a, en).testIn(backgroundScope, timeout = FLOW_TIMEOUT)
                val second = repository.statement(a, en).testIn(backgroundScope, timeout = FLOW_TIMEOUT)
                assertTrue(first.awaitItem().refreshing)
                assertTrue(second.awaitItem().refreshing)

                response.complete(statementA)

                listOf(first, second).forEach { collector ->
                    collector.awaitMatching { it.data == statementA && !it.refreshing }
                    collector.cancel()
                }
            }
            assertEquals(1, source.statementCalls)
        }

    @Test
    fun `prefetch fetches only the uncached statements, one at a time`() =
        runTest {
            cache(statementB)
            val calls = Channel<ProblemId>(Channel.UNLIMITED)
            val responses = listOf(a, c).associateWith { CompletableDeferred<Statement>() }
            source.onStatement = { problem, _ ->
                calls.send(problem)
                responses.getValue(problem).await()
            }

            val prefetch = statements().launchPrefetch(listOf(a, b, c), en)

            assertEquals(a, calls.receive())
            runCurrent()
            assertTrue("the next fetch waits for the first", calls.tryReceive().isFailure)
            responses.getValue(a).complete(statementA)
            assertEquals(c, calls.receive())
            responses.getValue(c).complete(statementC)
            prefetch.join()

            assertEquals(2, source.statementCalls)
            assertEquals(statementC, stored(c)?.toDomain())
        }

    @Test
    fun `prefetch stops at the first Offline`() =
        runTest {
            source.onStatement = { _, _ -> throw JudgeError.Offline() }

            statements().launchPrefetch(listOf(a, b, c), en).join()

            assertEquals(1, source.statementCalls)
        }

    @Test
    fun `prefetch skips a Malformed statement and fetches the next`() =
        runTest {
            source.onStatement = { problem, _ -> if (problem == a) throw JudgeError.Malformed() else statementB }

            statements().launchPrefetch(listOf(a, b), en).join()

            assertEquals(2, source.statementCalls)
            assertNull(stored(a))
            assertEquals(statementB, stored(b)?.toDomain())
        }

    @Test
    fun `prefetch shares the fetch a collector already started`() =
        runTest {
            val repository = statements()
            val response = CompletableDeferred<Statement>()
            source.onStatement = { _, _ -> response.await() }

            repository.statement(a, en).test(timeout = FLOW_TIMEOUT) {
                assertTrue(awaitItem().refreshing)
                assertEquals(1, source.statementCalls)

                val prefetch = repository.launchPrefetch(listOf(a), en)
                // Real time for the prefetch to read the cache (Room's own threads) and reach the judge, were it to.
                withContext(Dispatchers.Default) { delay(PREFETCH_SETTLE_MILLIS) }
                assertEquals(1, source.statementCalls)
                assertTrue("prefetch waits for the collector's fetch", prefetch.isActive)

                response.complete(statementA)
                prefetch.join()
                awaitMatching { it.data == statementA && !it.refreshing }
            }
            assertEquals(1, source.statementCalls)
        }

    @Test
    fun `pageUrl is the judge's problem URL`() =
        runTest {
            assertEquals(source.problemUrl(a, en), statements().pageUrl(a, en))
        }

    /** The raw English row of [problem], or null. */
    private suspend fun stored(problem: ProblemId): StatementEntity? {
        val rows = database.statementDao().statement(problem.contestId, problem.index, en.key)
        return rows.first()
    }

    private suspend fun TestScope.cache(statement: Statement) {
        database.statementDao().upsert(statement.toEntity(currentTime))
    }

    private fun TestScope.statements() =
        CachedStatementRepository(
            source,
            database.statementDao(),
            SchedulerClock(testScheduler),
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )

    private companion object {
        const val PREFETCH_SETTLE_MILLIS = 300L
        val en = StatementLanguage.En
        val a = ProblemId(1, "A")
        val b = ProblemId(1, "B")
        val c = ProblemId(1, "C")
        val statementA = statementOf(a)
        val statementB = statementOf(b)
        val statementC = statementOf(c)

        fun statementOf(problem: ProblemId) =
            Statement(
                problem = problem,
                language = StatementLanguage.En,
                title = "${problem.index}. Problem ${problem.display}",
                timeLimitMillis = 1_000,
                memoryLimitMegabytes = 256,
                inputFile = null,
                outputFile = null,
                sections = emptyList(),
                samples = emptyList(),
                materials = emptyList(),
            )
    }
}
