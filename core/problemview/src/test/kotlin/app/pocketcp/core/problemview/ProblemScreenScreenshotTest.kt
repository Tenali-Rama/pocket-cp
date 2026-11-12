package app.pocketcp.core.problemview

import androidx.compose.runtime.Composable
import app.pocketcp.core.datastore.StatementWidthPreference
import app.pocketcp.core.datastore.TextSizePreference
import app.pocketcp.core.model.JudgeError
import app.pocketcp.core.model.StatementLanguage
import app.pocketcp.core.screenshot.Device
import app.pocketcp.core.screenshot.ScreenshotConfig
import app.pocketcp.core.screenshot.ScreenshotTest
import app.pocketcp.core.screenshot.captureMatrix
import app.pocketcp.core.screenshot.captureSingle
import org.junit.Test

/** The problem view on 2274A's recorded statement in every state and reading setting (QUALITY §4). */
class ProblemScreenScreenshotTest : ScreenshotTest() {
    private val loaded = ProblemUiState(Paco, ProblemStatus.Loaded, recordedStatement())

    @Test
    fun problemView() = captureMatrix("problemView", PocketCpThemeWrapper) { Screen(loaded) }

    @Test
    fun russian() =
        single("problemView_ru") {
            Screen(loaded.copy(statement = recordedStatement(StatementLanguage.Ru), language = StatementLanguage.Ru))
        }

    @Test
    fun narrowTablet() =
        single("problemView_narrow", ScreenshotConfig(Device.Tablet)) {
            Screen(loaded.copy(width = StatementWidthPreference.Narrow))
        }

    @Test
    fun textLargest() = single("problemView_textLargest") { Screen(loaded.copy(textSize = TextSizePreference.Largest)) }

    @Test
    fun font150() = single("problemView_font150", ScreenshotConfig(fontScale = 1.5f)) { Screen(loaded) }

    @Test
    fun loading() = single("problemView_loading") { Screen(ProblemUiState(Paco, ProblemStatus.Loading)) }

    @Test
    fun error() =
        single("problemView_error") {
            Screen(ProblemUiState(Paco, ProblemStatus.Error, error = JudgeError.Offline()))
        }

    @Test
    fun staleError() = single("problemView_staleError") { Screen(loaded.copy(error = JudgeError.Offline())) }

    @Test
    fun unparsable() =
        single("problemView_unparsable") {
            Screen(ProblemUiState(Paco, ProblemStatus.Unparsable, error = JudgeError.Malformed()))
        }

    private fun single(
        name: String,
        config: ScreenshotConfig = ScreenshotConfig(),
        content: @Composable () -> Unit,
    ) = captureSingle(name, config, PocketCpThemeWrapper, content)
}

@Composable
private fun Screen(state: ProblemUiState) {
    ScreenshotFrame { ProblemScreen(state, RecordingActions(), onBack = {}) }
}
