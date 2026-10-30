@file:Suppress("MagicNumber") // This file is where the type-scale literals live.

package app.pocketcp.core.designsystem

import android.content.res.AssetManager
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The type scale. UI styles use the platform sans-serif; [code] and [codeSmall] use the bundled JetBrains Mono,
 * with ligatures on or off as set by `PocketCpTheme(codeLigatures = …)`. Sizes follow `PocketCpTheme(textScale = …)`
 * for the UI styles and `PocketCpTheme(codeTextScale = …)` for the code styles.
 */
@Immutable
data class PocketCpTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val code: TextStyle,
    val codeSmall: TextStyle,
)

/** JetBrains Mono (OFL), bundled in `assets/fonts/` next to its licence. */
internal fun jetBrainsMono(assets: AssetManager): FontFamily =
    FontFamily(
        Font("fonts/JetBrainsMono-Regular.ttf", assets, FontWeight.Normal),
        Font("fonts/JetBrainsMono-Bold.ttf", assets, FontWeight.Bold),
    )

/** CSS `font-feature-settings` that turn JetBrains Mono's ligatures (contextual alternates) off. */
internal const val NO_LIGATURES = "\"calt\" 0, \"liga\" 0"

/**
 * Builds the type scale. [textScale] multiplies the size and line height of every UI style,
 * [codeTextScale] those of [PocketCpTypography.code] and [PocketCpTypography.codeSmall].
 */
internal fun pocketCpTypography(
    codeFamily: FontFamily,
    codeLigatures: Boolean,
    textScale: Float = 1f,
    codeTextScale: Float = 1f,
): PocketCpTypography {
    val ui = TextStyle(fontFamily = FontFamily.SansSerif)
    val code = TextStyle(fontFamily = codeFamily, fontFeatureSettings = if (codeLigatures) null else NO_LIGATURES)

    fun TextStyle.sized(
        size: Int,
        line: Int,
        scale: Float,
    ) = copy(fontSize = size.sp * scale, lineHeight = line.sp * scale)
    return PocketCpTypography(
        display = ui.sized(32, 40, textScale).copy(fontWeight = FontWeight.Normal),
        headline = ui.sized(24, 32, textScale).copy(fontWeight = FontWeight.Normal),
        title = ui.sized(18, 24, textScale).copy(fontWeight = FontWeight.Medium),
        body = ui.sized(16, 24, textScale).copy(fontWeight = FontWeight.Normal),
        bodySmall = ui.sized(14, 20, textScale).copy(fontWeight = FontWeight.Normal),
        label = ui.sized(12, 16, textScale).copy(fontWeight = FontWeight.Medium),
        code = code.sized(14, 20, codeTextScale),
        codeSmall = code.sized(12, 16, codeTextScale),
    )
}

/** The Material 3 type scale built from ours, so stock Material components use the same sizes. */
internal fun PocketCpTypography.toMaterialTypography(): Typography =
    Typography(
        displayLarge = display,
        displayMedium = display,
        displaySmall = headline,
        headlineLarge = headline,
        headlineMedium = headline,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = body.copy(fontWeight = FontWeight.Medium),
        titleSmall = bodySmall.copy(fontWeight = FontWeight.Medium),
        bodyLarge = body,
        bodyMedium = bodySmall,
        bodySmall = label.copy(fontWeight = FontWeight.Normal),
        labelLarge = bodySmall.copy(fontWeight = FontWeight.Medium),
        labelMedium = label,
        labelSmall = label,
    )
