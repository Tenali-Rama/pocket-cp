package app.pocketcp.core.statement.math

/** The small, judge-independent AST used by the native statement math renderer. */
internal sealed interface TexNode {
    data class Sequence(
        val children: List<TexNode>,
    ) : TexNode

    data class Symbol(
        val value: String,
        val style: SymbolStyle = SymbolStyle.Normal,
    ) : TexNode

    data class Fraction(
        val numerator: TexNode,
        val denominator: TexNode,
    ) : TexNode

    data class Sqrt(
        val body: TexNode,
    ) : TexNode

    data class Scripts(
        val base: TexNode,
        val subscript: TexNode?,
        val superscript: TexNode?,
    ) : TexNode

    data class Delimited(
        val left: String,
        val body: TexNode,
        val right: String,
    ) : TexNode

    data class Binomial(
        val top: TexNode,
        val bottom: TexNode,
    ) : TexNode

    data class Matrix(
        val rows: List<List<TexNode>>,
        val cases: Boolean,
    ) : TexNode

    data class Text(
        val value: String,
    ) : TexNode
}

internal enum class SymbolStyle {
    Normal,
    Calligraphic,
    Text,
}

/**
 * A deliberately bounded TeX parser. Codeforces statements use a small math vocabulary; rejecting an unknown
 * command is preferable to silently displaying an incomplete equation.
 */
internal class TexParser(
    source: String,
) {
    private val cursor = TexCursor(source)

    fun parse(): TexNode = cursor.parseDocument()
}

internal class TexCursor(
    val source: String,
) {
    var position: Int = 0
}

internal data class ScriptPair(
    val subscript: TexNode?,
    val superscript: TexNode?,
)

internal class TexParseException(
    message: String,
) : IllegalArgumentException(message)
