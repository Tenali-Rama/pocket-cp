package app.pocketcp.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/** The app under test. */
internal const val PACKAGE_NAME = "app.pocketcp"

/** Every top-level destination, in nav order, ending back on Home. */
private val TAB_ROUTES = listOf("contests", "problemset", "profile", "settings", "home")

private const val TIMEOUT_MS = 10_000L

/**
 * Taps every top-level destination by its `nav_<route>` tag (a resource id, as Maestro sees it), so the same walk
 * drives the bottom bar on phones and the navigation rail on tablets. Fails when a tab is missing or never selects.
 */
internal fun MacrobenchmarkScope.walkTabs() {
    for (route in TAB_ROUTES) {
        val tag = "nav_$route"
        val item = device.wait(Until.findObject(By.res(tag)), TIMEOUT_MS) ?: error("No $tag on screen")
        item.click()
        check(device.wait(Until.hasObject(By.res(tag).selected(true)), TIMEOUT_MS)) { "$tag did not select" }
        device.waitForIdle()
    }
}
