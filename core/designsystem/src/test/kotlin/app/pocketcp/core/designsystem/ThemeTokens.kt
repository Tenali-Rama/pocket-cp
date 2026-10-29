package app.pocketcp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Windows at least this wide (Material 3 medium width) show the tokens in two columns. */
private val TwoColumnMinWidth = 600.dp

private const val CODE_SAMPLE = "if (a != b && i <= n) f = x -> x >= 0 // ==="

/** The token sheet: palette swatches, rank colours and the type scale of the current [PocketCpTheme]. */
@Composable
internal fun ThemeTokens(mode: ThemeMode) {
    val spacing = PocketCpTheme.spacing
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(PocketCpTheme.colors.background).padding(spacing.lg),
    ) {
        if (maxWidth >= TwoColumnMinWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                    Palette()
                    RankColours()
                }
                Column(Modifier.weight(1f)) { TypeScale(mode) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Palette()
                RankColours()
                TypeScale(mode)
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = PocketCpTheme.spacing
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(PocketCpTheme.colors.surface, PocketCpTheme.shapes.large)
                .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(title, style = PocketCpTheme.typography.title, color = PocketCpTheme.colors.onSurface)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Palette() {
    val c = PocketCpTheme.colors
    val swatches =
        listOf(
            "background" to c.background,
            "surface" to c.surface,
            "surfaceRaised" to c.surfaceRaised,
            "outline" to c.outline,
            "onBackground" to c.onBackground,
            "onSurface" to c.onSurface,
            "onSurfaceMuted" to c.onSurfaceMuted,
            "primary" to c.primary,
            "onPrimary" to c.onPrimary,
            "error" to c.error,
            "onError" to c.onError,
        )
    Section("Palette") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.sm),
        ) {
            swatches.forEach { (name, colour) -> Swatch(name, colour) }
        }
    }
}

@Composable
private fun Swatch(
    name: String,
    colour: Color,
) {
    val shape = PocketCpTheme.shapes.small
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.xs),
    ) {
        Box(
            Modifier
                .size(PocketCpTheme.spacing.lg)
                .background(colour, shape)
                .border(Dp.Hairline, PocketCpTheme.colors.onSurfaceMuted, shape),
        )
        Text(name, style = PocketCpTheme.typography.label, color = PocketCpTheme.colors.onSurfaceMuted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankColours() {
    Section("Rank colours") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(PocketCpTheme.spacing.xs),
        ) {
            Rank.entries.forEach { rank ->
                Text(
                    text = rankLabel(rank),
                    style = PocketCpTheme.typography.bodySmall,
                    color = PocketCpTheme.colors.rank.of(rank),
                )
            }
        }
    }
}

private fun rankLabel(rank: Rank): String {
    val name = rank.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
    val from = rank.minRating
    return when {
        from == null -> name
        from == Int.MIN_VALUE -> "$name <${Rank.Pupil.minRating}"
        else -> "$name $from+"
    }
}

@Composable
private fun TypeScale(mode: ThemeMode) {
    val t = PocketCpTheme.typography
    Section("Type") {
        TypeSample("Display 32", t.display)
        TypeSample("Headline 24", t.headline)
        TypeSample("Title 18", t.title)
        TypeSample("Body 16: the quick brown fox", t.body)
        TypeSample("Body small 14: jumps over the lazy dog", t.bodySmall)
        TypeSample("Label 12", t.label)
        TypeSample("Code, ligatures on", t.label, muted = true)
        TypeSample(CODE_SAMPLE, t.code)
        TypeSample("Code, ligatures off", t.label, muted = true)
        PocketCpTheme(mode = mode, codeLigatures = false) {
            TypeSample(CODE_SAMPLE, PocketCpTheme.typography.code)
        }
        TypeSample("Code small 12: for (int i = 0; i < n; ++i)", t.codeSmall)
    }
}

@Composable
private fun TypeSample(
    text: String,
    style: TextStyle,
    muted: Boolean = false,
) {
    val colour = if (muted) PocketCpTheme.colors.onSurfaceMuted else PocketCpTheme.colors.onSurface
    Text(text, style = style, color = colour)
}
