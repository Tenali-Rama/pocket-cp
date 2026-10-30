package app.pocketcp.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ITERATIONS = 5

/**
 * Frame timing while switching through every top-level tab; each iteration starts a fresh process on Home.
 * Not `startupMode = COLD`: that kills the process after `setupBlock`, so the walk would find no app.
 * `scripts/perf-budget-check.sh` finds `tabWalk` in the results by this method name.
 */
@RunWith(AndroidJUnit4::class)
class ShellJankBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun tabWalk() =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            iterations = ITERATIONS,
            setupBlock = {
                killProcess()
                startActivityAndWait()
            },
        ) {
            walkTabs()
        }
}
