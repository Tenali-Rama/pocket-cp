package app.pocketcp.core.statement.math

internal fun TexCursor.parseEnvironment(name: String): TexNode {
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

internal fun TexCursor.consumeCellSeparator() {
    if (position < source.length && source[position] == '&') position++
}

internal fun TexCursor.parseCell(): TexNode {
    val children = mutableListOf<TexNode>()
    while (hasCellContent()) children += parseScriptedAtom()
    return TexNode.Sequence(children)
}

internal fun TexCursor.hasCellContent(): Boolean {
    skipWhitespace()
    return when {
        position >= source.length -> false
        source[position] == '&' -> false
        source.startsWith("\\\\", position) -> false
        source.startsWith("\\end", position) -> false
        else -> true
    }
}
