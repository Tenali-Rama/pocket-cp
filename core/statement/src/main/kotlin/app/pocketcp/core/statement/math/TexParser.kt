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

private class TexCursor(
    val source: String,
) {
    var position: Int = 0
}

private data class ScriptPair(
    val subscript: TexNode?,
    val superscript: TexNode?,
)

private fun TexCursor.parseDocument(): TexNode {
    val result = parseSequence(stop = null)
    skipWhitespace()
    if (position != source.length) fail("unexpected '${source[position]}'")
    return result
}

private fun TexCursor.parseSequence(stop: Char?): TexNode {
    val children = mutableListOf<TexNode>()
    while (canParseSequence(stop)) children += parseScriptedAtom()
    if (stop != null && !atStop(stop)) fail("unbalanced group")
    return TexNode.Sequence(children)
}

private fun TexCursor.canParseSequence(stop: Char?): Boolean {
    skipWhitespace()
    return when {
        position >= source.length -> false
        source[position] == '}' && stop != '}' -> fail("unbalanced group")
        else -> source[position] != stop && !source.startsWith("\\right", position)
    }
}

private fun TexCursor.atStop(stop: Char): Boolean = position < source.length && source[position] == stop

private fun TexCursor.parseScriptedAtom(): TexNode {
    val atom = parseAtom()
    val scripts = parseScripts()
    return scripts?.let { TexNode.Scripts(atom, it.subscript, it.superscript) } ?: atom
}

private fun TexCursor.parseScripts(): ScriptPair? {
    var subscript: TexNode? = null
    var superscript: TexNode? = null
    while (hasScript()) {
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
    return if (subscript == null && superscript == null) null else ScriptPair(subscript, superscript)
}

private fun TexCursor.hasScript(): Boolean =
    position < source.length &&
        (source[position] == '_' || source[position] == '^')

private fun TexCursor.parseAtom(): TexNode {
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

private fun TexCursor.parseGroup(): TexNode {
    position++
    val group = parseSequence('}')
    if (position >= source.length || source[position] != '}') fail("unbalanced group")
    position++
    return group
}

private fun TexCursor.parseScript(): TexNode {
    skipWhitespace()
    if (position >= source.length) fail("missing script")
    return if (source[position] == '{') parseGroup() else parseAtom()
}

private fun TexCursor.parseCommand(): TexNode {
    position++
    val name = readCommandName()
    return parseArgumentCommand(name)
        ?: parseStructuralCommand(name)
        ?: TexNode.Symbol(COMMANDS[name] ?: fail("unknown command \\$name"))
}

private fun TexCursor.readCommandName(): String {
    if (position >= source.length) fail("incomplete command")
    if (!source[position].isLetter()) return source[position++].toString()
    val start = position
    while (position < source.length && source[position].isLetter()) position++
    return source.substring(start, position)
}

private fun TexCursor.parseArgumentCommand(name: String): TexNode? =
    when (name) {
        "frac" -> TexNode.Fraction(parseRequiredArgument(), parseRequiredArgument())
        "binom" -> TexNode.Binomial(parseRequiredArgument(), parseRequiredArgument())
        "sqrt" -> {
            if (peek() == '[') skipOptionalArgument()
            TexNode.Sqrt(parseRequiredArgument())
        }
        "text", "textrm", "mathrm" -> TexNode.Text(parseTextArgument())
        "mathcal" -> TexNode.Symbol(parseTextArgument(), SymbolStyle.Calligraphic)
        "mathbf" -> TexNode.Symbol(parseTextArgument(), SymbolStyle.Normal)
        else -> null
    }

private fun TexCursor.parseStructuralCommand(name: String): TexNode? =
    when (name) {
        "left" -> parseDelimited()
        "begin" -> parseEnvironment(parseTextArgument())
        else -> null
    }

private fun TexCursor.parseDelimited(): TexNode {
    val left = parseDelimiter()
    val body = parseSequence(stop = null)
    if (!source.startsWith("\\right", position)) fail("missing \\right")
    position += "\\right".length
    val right = parseDelimiter()
    return TexNode.Delimited(left, body, right)
}

private fun TexCursor.parseDelimiter(): String {
    skipWhitespace()
    if (position >= source.length) fail("missing delimiter")
    return if (source[position] != '\\') {
        source[position++].toString()
    } else {
        position++
        if (position < source.length && !source[position].isLetter()) {
            source[position++].toString()
        } else {
            val start = position
            while (position < source.length && source[position].isLetter()) position++
            val name = source.substring(start, position)
            DELIMITERS[name] ?: fail("unknown delimiter \\$name")
        }
    }
}

private fun TexCursor.parseEnvironment(name: String): TexNode {
    val cases = name == "cases"
    if (name != "matrix" && !cases) fail("unknown environment $name")
    val rows = mutableListOf<List<TexNode>>()
    var row = emptyList<TexNode>()
    while (position < source.length) {
        when {
            source.startsWith("\\end", position) -> {
                position += "\\end".length
                val endName = parseTextArgument()
                if (endName != name) fail("environment mismatch")
                rows += row
                return TexNode.Matrix(rows, cases)
            }
            source.startsWith("\\\\", position) -> {
                position += 2
                rows += row
                row = emptyList()
            }
            else -> {
                row = row + parseCell()
                consumeCellSeparator()
            }
        }
    }
    fail("missing \\end{$name}")
}

private fun TexCursor.consumeCellSeparator() {
    if (position < source.length && source[position] == '&') position++
}

private fun TexCursor.parseCell(): TexNode {
    val children = mutableListOf<TexNode>()
    while (hasCellContent()) children += parseScriptedAtom()
    return TexNode.Sequence(children)
}

private fun TexCursor.hasCellContent(): Boolean {
    skipWhitespace()
    return position < source.length &&
        source[position] != '&' &&
        !source.startsWith("\\\\", position) &&
        !source.startsWith("\\end", position)
}

private fun TexCursor.parseRequiredArgument(): TexNode {
    skipWhitespace()
    if (position >= source.length) fail("missing argument")
    return if (source[position] == '{') parseGroup() else parseAtom()
}

private fun TexCursor.parseTextArgument(): String {
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

private fun TexCursor.skipOptionalArgument() {
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

private fun TexCursor.skipWhitespace() {
    while (position < source.length && source[position].isWhitespace()) position++
}

private fun TexCursor.peek(): Char? = source.getOrNull(position)

private fun TexCursor.fail(message: String): Nothing = throw TexParseException(message)

internal class TexParseException(
    message: String,
) : IllegalArgumentException(message)
