package app.pocketcp.core.problemview

import androidx.compose.runtime.Composable
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.screenshot.Device
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

/**
 * The chrome around the Codeforces WebView: the unparsable statement's fallback and a contest material open over the
 * statement. The WebView itself draws nothing on the JVM.
 */
class WebPageScreenshotTest : ScreenshotTest() {
    private val fallback =
        ProblemUiState(
            Paco,
            ProblemStatus.Unparsable,
            error = JudgeError.Malformed(),
            pageUrl = FakeStatementRepository().pageUrl(Paco, StatementLanguage.En),
        )

    @Test
    fun fallbackPhone() = single("problemView_fallback", ScreenshotConfig(Device.Phone)) { Screen(fallback) }

    @Test
    fun fallbackTablet() = single("problemView_fallback", ScreenshotConfig(Device.Tablet)) { Screen(fallback) }

    @Test
    fun materialPageTablet() {
        val statement = recordedStatement()
        val tutorial = statement.materials.last()
        val state = ProblemUiState(Paco, ProblemStatus.Loaded, statement, webPage = tutorial.url)
        single("problemView_materialPage", ScreenshotConfig(Device.Tablet)) { Screen(state) }
    }

    private fun single(
        name: String,
        config: ScreenshotConfig,
        content: @Composable () -> Unit,
    ) = captureSingle(name, config, PocketCpThemeWrapper, content)
}

@Composable
private fun Screen(state: ProblemUiState) {
    ScreenshotFrame { ProblemScreen(state, RecordingActions(), onBack = {}) }
}
