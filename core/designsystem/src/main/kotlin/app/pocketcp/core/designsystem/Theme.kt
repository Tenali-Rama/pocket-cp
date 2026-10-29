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
 */
@Composable
fun PocketCpTheme(
    mode: ThemeMode = ThemeMode.Dark,
    codeLigatures: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = PocketCpColors.of(mode)
    val assets = LocalContext.current.assets
    val codeFamily = remember(assets) { jetBrainsMono(assets) }
    val typography = remember(codeFamily, codeLigatures) { pocketCpTypography(codeFamily, codeLigatures) }
    CompositionLocalProvider(
        LocalPocketCpColors provides colors,
        LocalPocketCpTypography provides typography,
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

    /** The same in every theme; read through the theme so a future density setting can change it in one place. */
    val spacing: PocketCpSpacing
        @Composable @ReadOnlyComposable
        get() = DefaultSpacing

    val shapes: PocketCpShapes
        @Composable @ReadOnlyComposable
        get() = DefaultShapes
}

private val DefaultSpacing = PocketCpSpacing()
private val DefaultShapes = PocketCpShapes()

private val LocalPocketCpColors =
    staticCompositionLocalOf<PocketCpColors> { error("PocketCpTheme.colors read outside PocketCpTheme") }

private val LocalPocketCpTypography =
    staticCompositionLocalOf<PocketCpTypography> { error("PocketCpTheme.typography read outside PocketCpTheme") }
