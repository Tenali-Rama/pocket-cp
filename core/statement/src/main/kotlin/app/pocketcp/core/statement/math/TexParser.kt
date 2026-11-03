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
    private val source: String,
) {
    private var position = 0

    fun parse(): TexNode {
        val result = parseSequence(stop = null)
        skipWhitespace()
        if (position != source.length) {
            fail("unexpected '${source[position]}'")
        }
        return result
    }

    private fun parseSequence(stop: Char?): TexNode {
        val children = mutableListOf<TexNode>()
        while (position < source.length) {
            skipWhitespace()
            if (position >= source.length || source[position] == stop) break
            if (source[position] == '}') {
                fail("unbalanced group")
            }
            if (source.startsWith("\\right", position)) {
                break
            }
            val atom = parseAtom()
            var subscript: TexNode? = null
            var superscript: TexNode? = null
            while (position < source.length && (source[position] == '_' || source[position] == '^')) {
                val kind = source[position++]
                val script = parseScript()
                if (kind == '_') {
                    if (subscript != null) fail("duplicate subscript")
                    subscript = script
                } else {
                    if (superscript != null) fail("duplicate superscript")
                    superscript = script
                }
            }
            children +=
                if (subscript == null && superscript == null) {
                    atom
                } else {
                    TexNode.Scripts(atom, subscript, superscript)
                }
        }
        if (stop != null && (position == source.length || source[position] != stop)) {
            fail("unbalanced group")
        }
        return TexNode.Sequence(children)
    }

    private fun parseAtom(): TexNode {
        skipWhitespace()
        if (position >= source.length) fail("missing expression")
        return when (val character = source[position]) {
            '{' -> parseGroup()
            '}' -> fail("unbalanced group")
            '_', '^' -> fail("script without a base")
            '\\' -> parseCommand()
            else -> {
                position++
                TexNode.Symbol(character.toString())
            }
        }
    }

    private fun parseGroup(): TexNode {
        position++
        val group = parseSequence('}')
        if (position >= source.length || source[position] != '}') fail("unbalanced group")
        position++
        return group
    }

    private fun parseScript(): TexNode {
        skipWhitespace()
        if (position >= source.length) fail("missing script")
        return if (source[position] == '{') parseGroup() else parseAtom()
    }

    private fun parseCommand(): TexNode {
        position++
        if (position >= source.length) fail("incomplete command")
        val name =
            if (source[position].isLetter()) {
                val start = position
                while (position < source.length && source[position].isLetter()) position++
                source.substring(start, position)
            } else {
                source[position++].toString()
            }
        return when (name) {
            "frac" -> TexNode.Fraction(parseRequiredArgument(), parseRequiredArgument())
            "binom" -> TexNode.Binomial(parseRequiredArgument(), parseRequiredArgument())
            "sqrt" -> {
                if (peek() == '[') skipOptionalArgument()
                TexNode.Sqrt(parseRequiredArgument())
            }
            "text", "textrm", "mathrm" -> TexNode.Text(parseTextArgument())
            "mathcal" -> TexNode.Symbol(parseTextArgument(), SymbolStyle.Calligraphic)
            "mathbf" -> TexNode.Symbol(parseTextArgument(), SymbolStyle.Normal)
            "left" -> parseDelimited()
            "begin" -> parseEnvironment(parseTextArgument())
            "quad" -> TexNode.Symbol("  ")
            "qquad" -> TexNode.Symbol("    ")
            "sum" -> TexNode.Symbol("∑")
            "prod" -> TexNode.Symbol("∏")
            else -> TexNode.Symbol(COMMANDS[name] ?: fail("unknown command \\$name"))
        }
    }

    private fun parseDelimited(): TexNode {
        val left = parseDelimiter()
        val body = parseSequence(stop = null)
        if (!source.startsWith("\\right", position)) fail("missing \\right")
        position += "\\right".length
        val right = parseDelimiter()
        return TexNode.Delimited(left, body, right)
    }

    private fun parseDelimiter(): String {
        skipWhitespace()
        if (position >= source.length) fail("missing delimiter")
        return if (source[position] == '\\') {
            position++
            if (position < source.length && !source[position].isLetter()) return source[position++].toString()
            val start = position
            while (position < source.length && source[position].isLetter()) position++
            val name = source.substring(start, position)
            DELIMITERS[name] ?: fail("unknown delimiter \\$name")
        } else {
            source[position++].toString()
        }
    }

    private fun parseEnvironment(name: String): TexNode {
        val cases = name == "cases"
        if (name != "matrix" && !cases) fail("unknown environment $name")
        val rows = mutableListOf<MutableList<TexNode>>()
        var row = mutableListOf<TexNode>()
        while (position < source.length) {
            if (source.startsWith("\\end", position)) {
                position += "\\end".length
                val endName = parseTextArgument()
                if (endName != name) fail("environment mismatch")
                rows += row
                return TexNode.Matrix(rows, cases)
            }
            if (source.startsWith("\\\\", position)) {
                position += 2
                rows += row
                row = mutableListOf()
                continue
            }
            val cell = parseCell()
            row += cell
            if (position < source.length && source[position] == '&') position++
        }
        fail("missing \\end{$name}")
    }

    private fun parseCell(): TexNode {
        val children = mutableListOf<TexNode>()
        while (position < source.length) {
            skipWhitespace()
            if (position >= source.length ||
                source[position] == '&' ||
                source.startsWith("\\\\", position) ||
                source.startsWith("\\end", position)
            ) {
                break
            }
            children += parseAtomWithScripts()
        }
        return TexNode.Sequence(children)
    }

    private fun parseAtomWithScripts(): TexNode {
        val atom = parseAtom()
        var sub: TexNode? = null
        var sup: TexNode? = null
        while (position < source.length && (source[position] == '_' || source[position] == '^')) {
            val kind = source[position++]
            val script = parseScript()
            if (kind == '_') sub = script else sup = script
        }
        return if (sub == null && sup == null) atom else TexNode.Scripts(atom, sub, sup)
    }

    private fun parseRequiredArgument(): TexNode {
        skipWhitespace()
        if (position >= source.length) fail("missing argument")
        return if (source[position] == '{') parseGroup() else parseAtom()
    }

    private fun parseTextArgument(): String {
        skipWhitespace()
        if (position >= source.length || source[position] != '{') fail("missing argument")
        position++
        val start = position
        var depth = 1
        while (position < source.length && depth > 0) {
            when (source[position++]) {
                '{' -> depth++
                '}' -> depth--
            }
        }
        if (depth != 0) fail("unbalanced group")
        return source.substring(start, position - 1).replace(Regex("\\\\([{}])"), "$1")
    }

    private fun skipOptionalArgument() {
        var depth = 0
        while (position < source.length) {
            when (source[position++]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return
                }
            }
        }
        fail("unbalanced optional argument")
    }

    private fun skipWhitespace() {
        while (position < source.length && source[position].isWhitespace()) position++
    }

    private fun peek(): Char? = source.getOrNull(position)

    private fun fail(message: String): Nothing = throw TexParseException(message)

    companion object {
        private val COMMANDS =
            mapOf(
                "alpha" to "α",
                "beta" to "β",
                "gamma" to "γ",
                "delta" to "δ",
                "epsilon" to "ε",
                "varepsilon" to "ϵ",
                "theta" to "θ",
                "lambda" to "λ",
                "mu" to "μ",
                "pi" to "π",
                "rho" to "ρ",
                "sigma" to "σ",
                "tau" to "τ",
                "phi" to "φ",
                "varphi" to "ϕ",
                "chi" to "χ",
                "psi" to "ψ",
                "omega" to "ω",
                "Gamma" to "Γ",
                "Delta" to "Δ",
                "Theta" to "Θ",
                "Lambda" to "Λ",
                "Pi" to "Π",
                "Sigma" to "Σ",
                "Phi" to "Φ",
                "Psi" to "Ψ",
                "Omega" to "Ω",
                "le" to "≤",
                "leq" to "≤",
                "ge" to "≥",
                "geq" to "≥",
                "ne" to "≠",
                "neq" to "≠",
                "cdot" to "⋅",
                "ldots" to "…",
                "dots" to "…",
                "cdots" to "⋯",
                "pm" to "±",
                "times" to "×",
                "infty" to "∞",
                "to" to "→",
                "rightarrow" to "→",
                "leftarrow" to "←",
                "iff" to "⇔",
                "in" to "∈",
                "notin" to "∉",
                "oplus" to "⊕",
                "otimes" to "⊗",
                "cap" to "∩",
                "cup" to "∪",
                "subset" to "⊂",
                "subseteq" to "⊆",
                "forall" to "∀",
                "exists" to "∃",
                "partial" to "∂",
                "nabla" to "∇",
                "mid" to "∣",
                "vert" to "|",
                "colon" to ":",
                "lfloor" to "⌊",
                "rfloor" to "⌋",
                "lceil" to "⌈",
                "rceil" to "⌉",
                "log" to "log",
                "ln" to "ln",
                "sin" to "sin",
                "cos" to "cos",
                "tan" to "tan",
                "min" to "min",
                "max" to "max",
                "lim" to "lim",
                "{" to "{",
                "}" to "}",
                "_" to "_",
                " " to " ",
                "," to " ",
                ";" to " ",
                ":" to ":",
            )

        private val DELIMITERS =
            mapOf(
                "langle" to "⟨",
                "rangle" to "⟩",
                "lfloor" to "⌊",
                "rfloor" to "⌋",
                "lceil" to "⌈",
                "rceil" to "⌉",
                "vert" to "|",
                "|" to "|",
                "." to "",
            )
    }
}

internal class TexParseException(
    message: String,
) : IllegalArgumentException(message)
