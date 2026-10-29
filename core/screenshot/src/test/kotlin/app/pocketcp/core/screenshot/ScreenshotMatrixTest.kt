package app.pocketcp.core.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.roborazziSystemPropertyTaskType
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Self-test of the matrix: its 8 goldens show the sample filling each device window in each theme. */
class ScreenshotMatrixTest : ScreenshotTest() {
    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun sample_rendersOnEveryDeviceInDarkAndLight() {
        val seen = mutableListOf<String>()

        captureMatrix("sample") { Sample(onRender = { seen += it }) }

        assumeTrue("captures only run under Roborazzi record/verify", roborazziSystemPropertyTaskType().isEnabled())
        val expected =
            Device.entries.flatMap { device ->
                listOf(true, false).map { dark -> describe(device.widthDp, device.heightDp, dark) }
            }
        assertEquals(expected, seen.distinct())
    }
}

private fun describe(
    widthDp: Int,
    heightDp: Int,
    dark: Boolean,
) = "$widthDp × $heightDp dp · ${if (dark) "dark" else "light"}"

/** Fills the window with the theme's container colour and names the window size and theme it sees. */
@Composable
private fun Sample(onRender: (String) -> Unit) {
    val configuration = LocalConfiguration.current
    val label = describe(configuration.screenWidthDp, configuration.screenHeightDp, isSystemInDarkTheme())
    onRender(label)
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
