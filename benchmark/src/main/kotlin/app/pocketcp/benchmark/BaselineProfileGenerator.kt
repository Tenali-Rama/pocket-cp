package app.pocketcp.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the app's Baseline Profile: launch, a walk through every tab, then a statement (2274A, opened from
 * Problemset) flung down and up, the jank-prone native LaTeX path. CI's perf job runs it
 * (`:app:generateBaselineProfile`) and uploads the result as the `baseline-profile` artifact; the committed copy is
 * `app/src/main/baseline-prof.txt`. Never generate it locally (ADR 0013).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()
            walkTabs()
            openTab("problemset")
            openProblem("2274A")
            flingStatement()
        }
}
