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
    return PocketCpTypography(
        display = ui.copy(fontSize = 32.sp * textScale, lineHeight = 40.sp * textScale, fontWeight = FontWeight.Normal),
        headline = ui.copy(fontSize = 24.sp * textScale, lineHeight = 32.sp * textScale, fontWeight = FontWeight.Normal),
        title = ui.copy(fontSize = 18.sp * textScale, lineHeight = 24.sp * textScale, fontWeight = FontWeight.Medium),
        body = ui.copy(fontSize = 16.sp * textScale, lineHeight = 24.sp * textScale, fontWeight = FontWeight.Normal),
        bodySmall = ui.copy(fontSize = 14.sp * textScale, lineHeight = 20.sp * textScale, fontWeight = FontWeight.Normal),
        label = ui.copy(fontSize = 12.sp * textScale, lineHeight = 16.sp * textScale, fontWeight = FontWeight.Medium),
        code = code.copy(fontSize = 14.sp * codeTextScale, lineHeight = 20.sp * codeTextScale),
        codeSmall = code.copy(fontSize = 12.sp * codeTextScale, lineHeight = 16.sp * codeTextScale),
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
