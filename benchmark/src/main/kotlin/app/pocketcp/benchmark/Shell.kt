package app.pocketcp.benchmark

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** The app under test. */
internal const val PACKAGE_NAME = "app.pocketcp"

/** Every top-level destination, in nav order, ending back on Home. */
private val TAB_ROUTES = listOf("contests", "problemset", "profile", "settings", "home")

private const val TIMEOUT_MS = 10_000L

private const val POLL_MS = 100L

/** `core/problemview`'s statement content (`PROBLEM_VIEW_TAG`). */
private const val PROBLEM_VIEW = "problem_view"

/** Flings start this share of the screen away from its edges, clear of the system gesture areas. */
private const val GESTURE_MARGIN_DIVISOR = 5

/**
 * Taps every top-level destination by its `nav_<route>` tag (a resource id, as Maestro sees it), so the same walk
 * drives the bottom bar on phones and the navigation rail on tablets. Fails when a tab is missing or never selects.
 */
internal fun MacrobenchmarkScope.walkTabs() {
    for (route in TAB_ROUTES) openTab(route)
}

/** Taps the `nav_<route>` destination and waits until it is selected. */
internal fun MacrobenchmarkScope.openTab(route: String) {
    val tag = "nav_$route"
    find(tag).click()
    check(device.wait(Until.hasObject(By.res(tag).selected(true)), TIMEOUT_MS)) { "$tag did not select" }
    device.waitForIdle()
}

/**
 * Taps Problemset's `problem_<display>` row and waits for its statement: a page on phones (the row is gone), beside
 * the list on tablets (the row is selected; another problem's statement was already shown there).
 */
internal fun MacrobenchmarkScope.openProblem(display: String) {
    val tag = "problem_$display"
    find(tag).click()
    check(awaitOpened(tag)) { "$tag did not open" }
    find(PROBLEM_VIEW)
    device.waitForIdle()
}

/**
 * Whether row [tag] opened within [TIMEOUT_MS]: gone (a phone's page covers it) or marked selected. Compose reports
 * `selected` on a row without the Tab role as accessibility `checked`, so both count.
 */
private fun MacrobenchmarkScope.awaitOpened(tag: String): Boolean {
    val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
    while (SystemClock.uptimeMillis() < deadline) {
        val opened =
            !device.hasObject(By.res(tag)) ||
                device.hasObject(By.res(tag).checked(true)) ||
                device.hasObject(By.res(tag).selected(true))
        if (opened) return true
        SystemClock.sleep(POLL_MS)
    }
    return false
}

/** Back from a phone's statement page to the Problemset list; a tablet already shows the list beside it. */
internal fun MacrobenchmarkScope.backToProblemList(display: String) {
    if (!device.hasObject(By.res("problem_$display"))) device.pressBack()
    find("problem_$display")
}

/**
 * Flings the open statement down [downs] times, then up once. Finds it again for each fling: a recomposition can
 * replace its node, and a held [UiObject2] then goes stale.
 */
internal fun MacrobenchmarkScope.flingStatement(downs: Int = 3) {
    repeat(downs) { flingStatementOnce(Direction.DOWN) }
    flingStatementOnce(Direction.UP)
}

private fun MacrobenchmarkScope.flingStatementOnce(direction: Direction) {
    val statement = find(PROBLEM_VIEW)
    statement.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
    statement.fling(direction)
    device.waitForIdle()
}

/** The object tagged [tag] (a resource id), waiting for it; fails when it never shows. */
private fun MacrobenchmarkScope.find(tag: String): UiObject2 =
    device.wait(Until.findObject(By.res(tag)), TIMEOUT_MS) ?: error("No $tag on screen")
