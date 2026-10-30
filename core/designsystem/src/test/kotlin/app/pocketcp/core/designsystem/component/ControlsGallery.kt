package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.pocketcp.core.designsystem.PocketCpTheme

/** Windows at least this wide (Material 3 medium width) show the two groups side by side. */
private val WideMinWidth = 600.dp

/** The text-size options of the Settings screen, as a sample single-choice group. */
private val TextSizes = listOf("Small", "Default", "Large", "Largest")

/** Switch rows (on, off, disabled on and off) and a four-option choice group, as on the Settings screen. */
@Composable
internal fun ControlsGallery() {
    val spacing = PocketCpTheme.spacing
    Column(Modifier.fillMaxSize().background(PocketCpTheme.colors.background)) {
        PcpTopAppBar(title = "Settings", onNavigateBack = {})
        BoxWithConstraints(Modifier.padding(horizontal = spacing.lg)) {
            if (maxWidth >= WideMinWidth) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xl)) {
                    Column(Modifier.weight(1f)) { Switches() }
                    Column(Modifier.weight(1f)) { Choices() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Switches()
                    Choices()
                }
            }
        }
    }
}

@Composable
private fun Switches() {
    Section("Features") {
        PcpSwitchRow("AI assistant", checked = true, onCheckedChange = {}, supporting = "Hints and explanations")
        PcpSwitchRow("Stress tester", checked = false, onCheckedChange = {})
        PcpSwitchRow("Local compute", checked = true, onCheckedChange = {}, supporting = "Not on this device", enabled = false)
        PcpSwitchRow("Focus mode auto-start", checked = false, onCheckedChange = {}, enabled = false)
    }
}

@Composable
private fun Choices() {
    Section("Text size") {
        Column(Modifier.selectableGroup()) {
            TextSizes.forEach { size -> PcpChoiceRow(size, selected = size == "Default", onClick = {}) }
        }
    }
}

@Composable
private fun Section(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier.padding(vertical = PocketCpTheme.spacing.sm),
        style = PocketCpTheme.typography.label,
        color = PocketCpTheme.colors.onSurfaceMuted,
    )
    PcpCard(Modifier.fillMaxWidth(), content = content)
}
