package app.pocketcp.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ITERATIONS = 10

/**
 * Cold and warm start to the shell's first frame, with the shipped Baseline Profile required.
 * `scripts/perf-budget-check.sh` finds `startupCold` and `startupWarm` in the results by these method names.
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCold() = startup(StartupMode.COLD)

    @Test
    fun startupWarm() = startup(StartupMode.WARM)

    private fun startup(mode: StartupMode) =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Max)),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = mode,
            iterations = ITERATIONS,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
        }
}
