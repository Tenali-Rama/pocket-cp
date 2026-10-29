@file:Suppress("MagicNumber") // This file is where the colour literals live.

package app.pocketcp.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The three themes. [Dark] is the default; [Amoled] is dark on pure black. */
enum class ThemeMode { Dark, Light, Amoled }

/**
 * The near-monochrome palette of one [ThemeMode]: greys only, with [rank] colours as the only strong accent.
 * Every `on*` colour is text on the colour it names and reaches WCAG AA (4.5:1) on it.
 * [onSurfaceMuted] (secondary text) and [error] are readable on [background], [surface] and [surfaceRaised].
 */
@Immutable
data class PocketCpColors(
    val isDark: Boolean,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val error: Color,
    val onError: Color,
    val rank: RankColors,
) {
    companion object {
        /** The palette of [mode]. */
        fun of(mode: ThemeMode): PocketCpColors =
            when (mode) {
                ThemeMode.Dark -> DarkColors
                ThemeMode.Light -> LightColors
                ThemeMode.Amoled -> AmoledColors
            }
    }
}

/** Rank shades for the dark and AMOLED themes: Codeforces hues lifted to read on near-black. */
private val DarkRankColors =
    RankColors(
        newbie = Color(0xFFA6A6A6),
        pupil = Color(0xFF5CC45C),
        specialist = Color(0xFF2EC4B6),
        expert = Color(0xFF7A98FF),
        candidateMaster = Color(0xFFD873D8),
        master = Color(0xFFFF9A2E),
        grandmaster = Color(0xFFFF6161),
        unrated = Color(0xFFEDEDED),
    )

/** Rank shades for the light theme: Codeforces hues, darkened where needed to read on white. */
private val LightRankColors =
    RankColors(
        newbie = Color(0xFF616161),
        pupil = Color(0xFF007A00),
        specialist = Color(0xFF00736A),
        expert = Color(0xFF0000FF),
        candidateMaster = Color(0xFFAA00AA),
        master = Color(0xFFA35200),
        grandmaster = Color(0xFFD50000),
        unrated = Color(0xFF141414),
    )

private val DarkColors =
    PocketCpColors(
        isDark = true,
        background = Color(0xFF121212),
        onBackground = Color(0xFFEDEDED),
        surface = Color(0xFF1C1C1C),
        surfaceRaised = Color(0xFF262626),
        onSurface = Color(0xFFEDEDED),
        onSurfaceMuted = Color(0xFFA8A8A8),
        outline = Color(0xFF3A3A3A),
        primary = Color(0xFFEDEDED),
        onPrimary = Color(0xFF121212),
        error = Color(0xFFFF8A80),
        onError = Color(0xFF121212),
        rank = DarkRankColors,
    )

private val AmoledColors =
    DarkColors.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceRaised = Color(0xFF141414),
        outline = Color(0xFF2E2E2E),
        onPrimary = Color.Black,
        onError = Color.Black,
    )

private val LightColors =
    PocketCpColors(
        isDark = false,
        background = Color(0xFFF7F7F7),
        onBackground = Color(0xFF141414),
        surface = Color.White,
        surfaceRaised = Color(0xFFEDEDED),
        onSurface = Color(0xFF141414),
        onSurfaceMuted = Color(0xFF5C5C5C),
        outline = Color(0xFFD0D0D0),
        primary = Color(0xFF141414),
        onPrimary = Color.White,
        error = Color(0xFFB3261E),
        onError = Color.White,
        rank = LightRankColors,
    )

/** The Material 3 scheme matching these colours, so stock Material components stay monochrome too. */
internal fun PocketCpColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = surfaceRaised,
        onPrimaryContainer = onSurface,
        secondary = onSurfaceMuted,
        onSecondary = surface,
        secondaryContainer = surfaceRaised,
        onSecondaryContainer = onSurface,
        tertiary = onSurfaceMuted,
        onTertiary = surface,
        tertiaryContainer = surfaceRaised,
        onTertiaryContainer = onSurface,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = onSurfaceMuted,
        surfaceTint = Color.Transparent,
        surfaceBright = surfaceRaised,
        surfaceDim = background,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        inversePrimary = surface,
        outline = outline,
        outlineVariant = outline,
        error = error,
        onError = onError,
        errorContainer = error,
        onErrorContainer = onError,
        scrim = Color.Black,
    )
}
