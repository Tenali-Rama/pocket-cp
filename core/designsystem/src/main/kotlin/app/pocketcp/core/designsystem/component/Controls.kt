package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import app.pocketcp.core.designsystem.PocketCpTheme

/**
 * A settings row with a [title], optional [supporting] line and a switch at the end. The whole row is one
 * toggle with the switch role: a tap anywhere, or `Enter` / `Space` when focused, flips [checked].
 * A disabled row mutes its title and ignores input.
 */
@Composable
fun PcpSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    val colors = PocketCpTheme.colors
    // An enabled "off" switch keeps a visible outline: Material's default `outline` is too faint on dark surfaces.
    val switchColors =
        SwitchDefaults.colors(uncheckedThumbColor = colors.onSurfaceMuted, uncheckedBorderColor = colors.onSurfaceMuted)
    PcpRow(
        headline = title,
        supporting = supporting,
        enabled = enabled,
        modifier =
            modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = switchColors) },
    )
}

/**
 * One option of a single-choice group: a radio button, then the [title]. The whole row is one radio button;
 * a tap, or `Enter` / `Space` when focused, calls [onClick]. Put the rows of one group in a container with
 * `Modifier.selectableGroup()` so TalkBack reads them as a group.
 */
@Composable
fun PcpChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PcpRow(
        headline = title,
        enabled = enabled,
        modifier =
            modifier.selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        leading = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
    )
}
