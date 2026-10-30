package app.pocketcp.core.data

import app.pocketcp.core.model.JudgeError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Where a [CachedResource] reads and writes one dataset. */
internal interface CachedDataset<T> {
    /** The Room cache, re-emitting on every write. */
    fun cached(): Flow<T>

    /** The dataset's `cache_meta` time, or null if never fetched. */
    suspend fun fetchedAtMillis(): Long?

    /** Reads the judge and swaps the cache. Throws only `JudgeError`. */
    suspend fun fetch()
}

/**
 * One stale-while-revalidate dataset (ADR 0010) over [dataset]: stale after [maxAge] by [clock].
 * Refreshes run in [scope], at most one at a time; [dispatcher] maps and combines emissions off the main thread.
 */
internal class CachedResource<T>(
    private val dataset: CachedDataset<T>,
    private val maxAge: Duration,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    private data class Status(
        val refreshing: Boolean = false,
        val error: JudgeError? = null,
    )

    private val status = MutableStateFlow(Status())
    private val mutex = Mutex()
    private var inFlight: Job? = null

    /** The cache at once, then every change to it or to the refresh status. Starts a refresh when stale. */
    fun stream(): Flow<Cached<T>> =
        flow {
            start(force = false)
            val snapshots =
                dataset.cached().map { data -> data to dataset.fetchedAtMillis()?.let(Instant::ofEpochMilli) }
            val cachedWithStatus =
                combine(snapshots, status) { (data, fetchedAt), current ->
                    Cached(data.takeIf { fetchedAt != null }, fetchedAt, current.refreshing, current.error)
                }
            emitAll(cachedWithStatus)
        }.distinctUntilChanged().flowOn(dispatcher)

    /** Refreshes when stale, or always when [force]; joins a refresh already running. Failures go to [stream]. */
    suspend fun refresh(force: Boolean) {
        start(force)?.join()
    }

    private suspend fun start(force: Boolean): Job? =
        mutex.withLock {
            inFlight?.takeIf { it.isActive }?.let { return it }
            if (!force && isFresh()) return null
            status.update { it.copy(refreshing = true) }
            scope.launch { run() }.also { inFlight = it }
        }

    /** Fresh when fetched at most [maxAge] ago. A fetch time in the future (the clock went back) counts as stale. */
    private suspend fun isFresh(): Boolean {
        val fetchedAt = dataset.fetchedAtMillis() ?: return false
        return clock.millis() - fetchedAt in 0..maxAge.toMillis()
    }

    private suspend fun run() {
        try {
            dataset.fetch()
            status.value = Status()
        } catch (error: JudgeError) {
            status.value = Status(error = error)
        } finally {
            status.update { it.copy(refreshing = false) }
        }
    }
}
