package app.pocketcp.feature.problemset

import app.cash.turbine.ReceiveTurbine

/** Skips states until one matches [predicate]: the first item may be the initial state before the data arrives. */
internal suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
    while (true) {
        val state = awaitItem()
        if (predicate(state)) return state
    }
}
