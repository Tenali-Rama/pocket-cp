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

/** Fixture problems with statements in `testdata/codeforces/pages/` (ADR 0015): the measured one and another. */
private const val MEASURED = "2274A"
private const val OTHER = "2273D"

/**
 * Frame timing while scrolling a cached statement (spec §3: no dropped frames scrolling statements, with native
 * LaTeX and images). Each iteration starts a fresh process and opens [MEASURED] once, which caches it, then
 * [OTHER]; the measured block opens [MEASURED] again from the cache and flings it down three times and up once.
 * `scripts/perf-budget-check.sh` finds `statementScroll` in the results by this method name.
 */
@RunWith(AndroidJUnit4::class)
class StatementBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun statementScroll() =
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            iterations = ITERATIONS,
            setupBlock = {
                killProcess()
                startActivityAndWait()
                openTab("problemset")
                openProblem(MEASURED)
                backToProblemList(OTHER)
                openProblem(OTHER)
                backToProblemList(MEASURED)
            },
        ) {
            openProblem(MEASURED)
            flingStatement()
        }
}
