package app.pocketcp.core.statement.math

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.pocketcp.core.designsystem.PocketCpTheme

/** A replacement point for native TeX rendering in the next statement milestone. */
@Immutable
interface MathRenderer {
    /** Renders TeX inside a line of statement text. */
    @Composable
    fun Inline(tex: String)

    /** Renders TeX as a statement block. */
    @Composable
    fun Display(tex: String)
}

/** The statement's current math implementation. 045 replaces only the implementation behind this local. */
val LocalMathRenderer = staticCompositionLocalOf<MathRenderer> { SourceMathRenderer }

private object SourceMathRenderer : MathRenderer {
    @Composable
    override fun Inline(tex: String) {
        Text(
            text = tex,
            style = PocketCpTheme.typography.code,
            color = PocketCpTheme.colors.onSurface,
            modifier =
                androidx.compose.ui.Modifier
                    .semantics { contentDescription = "math" },
        )
    }

    @Composable
    override fun Display(tex: String) {
        Text(
            text = tex,
            style = PocketCpTheme.typography.code,
            color = PocketCpTheme.colors.onBackground,
            modifier =
                androidx.compose.ui.Modifier
                    .semantics { contentDescription = "math" },
        )
    }
}
