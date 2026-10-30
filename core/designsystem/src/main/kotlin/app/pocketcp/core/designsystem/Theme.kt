package app.pocketcp.core.designsystem

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * The app theme: provides [PocketCpTheme.colors], [PocketCpTheme.typography], [PocketCpTheme.spacing] and
 * [PocketCpTheme.shapes], and a matching [MaterialTheme] for stock Material 3 components.
 * It draws nothing itself; the screen paints `PocketCpTheme.colors.background`.
 *
 * @param mode [ThemeMode.Dark] (default), [ThemeMode.Light] or [ThemeMode.Amoled].
 * @param codeLigatures whether the code font (JetBrains Mono) renders ligatures such as `!=` and `->`.
 * @param density [UiDensity.Comfortable] (default) or [UiDensity.Compact], which trims the vertical padding of
 *   rows and cards through [PocketCpTheme.spacing]. Touch targets stay at least `spacing.touchTarget`.
 * @param textScale multiplies the size of every UI text style; it stacks with the system font scale.
 * @param codeTextScale multiplies the size of `typography.code` and `typography.codeSmall` only.
 */
@Composable
fun PocketCpTheme(
    mode: ThemeMode = ThemeMode.Dark,
    codeLigatures: Boolean = true,
    density: UiDensity = UiDensity.Comfortable,
    textScale: Float = 1f,
    codeTextScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = PocketCpColors.of(mode)
    val assets = LocalContext.current.assets
    val codeFamily = remember(assets) { jetBrainsMono(assets) }
    val typography =
        remember(codeFamily, codeLigatures, textScale, codeTextScale) {
            pocketCpTypography(codeFamily, codeLigatures, textScale, codeTextScale)
        }
    CompositionLocalProvider(
        LocalPocketCpColors provides colors,
        LocalPocketCpTypography provides typography,
        LocalPocketCpSpacing provides PocketCpSpacing.of(density),
    ) {
        MaterialTheme(
            colorScheme = remember(colors) { colors.toMaterialColorScheme() },
            typography = remember(typography) { typography.toMaterialTypography() },
            shapes = remember { DefaultShapes.toMaterialShapes() },
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
        }
    }
}

/** The current theme's tokens. Read them only inside [PocketCpTheme]. */
object PocketCpTheme {
    val colors: PocketCpColors
        @Composable @ReadOnlyComposable
        get() = LocalPocketCpColors.current

    val typography: PocketCpTypography
        @Composable @ReadOnlyComposable
        get() = LocalPocketCpTypography.current

    /** The same in every colour theme; the row and card tokens follow `PocketCpTheme(density = …)`. */
    val spacing: PocketCpSpacing
        @Composable @ReadOnlyComposable
        get() = LocalPocketCpSpacing.current

    val shapes: PocketCpShapes
        @Composable @ReadOnlyComposable
        get() = DefaultShapes
}

private val DefaultShapes = PocketCpShapes()

private val LocalPocketCpColors =
    staticCompositionLocalOf<PocketCpColors> { error("PocketCpTheme.colors read outside PocketCpTheme") }

/** Defaults to comfortable spacing, so layouts that read spacing outside [PocketCpTheme] keep working as before. */
private val LocalPocketCpSpacing = staticCompositionLocalOf { PocketCpSpacing.of(UiDensity.Comfortable) }

private val LocalPocketCpTypography =
    staticCompositionLocalOf<PocketCpTypography> { error("PocketCpTheme.typography read outside PocketCpTheme") }
