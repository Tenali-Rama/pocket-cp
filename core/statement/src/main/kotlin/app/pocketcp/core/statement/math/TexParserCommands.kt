package app.pocketcp.core.statement.math

internal fun TexCursor.parseCommand(): TexNode {
    position++
    val name = readCommandName()
    return parseArgumentCommand(name)
        ?: parseStructuralCommand(name)
        ?: TexNode.Symbol(COMMANDS[name] ?: fail("unknown command \\$name"))
}

internal fun TexCursor.readCommandName(): String {
    if (position >= source.length) fail("incomplete command")
    if (!source[position].isLetter()) return source[position++].toString()
    val start = position
    while (position < source.length && source[position].isLetter()) position++
    return source.substring(start, position)
}

internal fun TexCursor.parseArgumentCommand(name: String): TexNode? =
    when (name) {
        "frac" -> TexNode.Fraction(parseRequiredArgument(), parseRequiredArgument())
        "binom" -> TexNode.Binomial(parseRequiredArgument(), parseRequiredArgument())
        "sqrt" -> {
            val index = if (peek() == '[') parseOptionalArgument() else null
            TexNode.Sqrt(parseRequiredArgument(), index)
        }
        "text", "textrm", "mathrm" -> TexNode.Text(parseTextArgument())
        "mathcal" -> TexNode.Symbol(parseTextArgument(), SymbolStyle.Calligraphic)
        "mathbf" -> TexNode.Symbol(parseTextArgument(), SymbolStyle.Bold)
        else -> null
    }

internal fun TexCursor.parseStructuralCommand(name: String): TexNode? =
    when (name) {
        "left" -> parseDelimited()
        "begin" -> parseEnvironment(parseTextArgument())
        else -> null
    }

internal fun TexCursor.parseDelimited(): TexNode {
    val left = parseDelimiter()
    val body = parseSequence(stop = null)
    if (!source.startsWith("\\right", position)) fail("missing \\right")
    position += "\\right".length
    val right = parseDelimiter()
    return TexNode.Delimited(left, body, right)
}

internal fun TexCursor.parseDelimiter(): String {
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

internal fun TexCursor.parseRequiredArgument(): TexNode {
    skipWhitespace()
    if (position >= source.length) fail("missing argument")
    return if (source[position] == '{') parseGroup() else parseAtom()
}

internal fun TexCursor.parseTextArgument(): String {
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

internal fun TexCursor.parseOptionalArgument(): TexNode {
    position++
    val argument = parseSequence(']')
    position++
    return argument
}
