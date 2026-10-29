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
 * with ligatures on or off as set by `PocketCpTheme(codeLigatures = …)`.
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

internal fun pocketCpTypography(
    codeFamily: FontFamily,
    codeLigatures: Boolean,
): PocketCpTypography {
    val ui = TextStyle(fontFamily = FontFamily.SansSerif)
    val code = TextStyle(fontFamily = codeFamily, fontFeatureSettings = if (codeLigatures) null else NO_LIGATURES)
    return PocketCpTypography(
        display = ui.copy(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal),
        headline = ui.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal),
        title = ui.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
        body = ui.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodySmall = ui.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        label = ui.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        code = code.copy(fontSize = 14.sp, lineHeight = 20.sp),
        codeSmall = code.copy(fontSize = 12.sp, lineHeight = 16.sp),
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
