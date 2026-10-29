package app.pocketcp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.pocketcp.core.designsystem.PocketCpTheme

/** The main action of a screen or section: filled with `colors.primary`. */
@Composable
fun PcpPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PocketCpTheme.shapes.medium,
    ) {
        ButtonLabel(text)
    }
}

/** A secondary action next to a [PcpPrimaryButton]: outlined, no fill. */
@Composable
fun PcpSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = PocketCpTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PocketCpTheme.shapes.medium,
        border = BorderStroke(Dp.Hairline, if (enabled) colors.onSurfaceMuted else colors.outline),
    ) {
        ButtonLabel(text)
    }
}

/** A low-emphasis action, such as "Show all": text only. */
@Composable
fun PcpTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = PocketCpTheme.shapes.medium,
    ) {
        ButtonLabel(text)
    }
}

/**
 * An icon-only action. [contentDescription] is required: it is all TalkBack has to read.
 * The touch target is at least `spacing.touchTarget` (48 dp) whatever the icon size.
 */
@Composable
fun PcpIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val touchTarget = PocketCpTheme.spacing.touchTarget
    IconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = touchTarget, minHeight = touchTarget),
        enabled = enabled,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun ButtonLabel(text: String) {
    Text(text = text, style = PocketCpTheme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
