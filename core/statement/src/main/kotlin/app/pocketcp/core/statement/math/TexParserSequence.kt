package app.pocketcp.core.statement.math

internal fun TexCursor.parseDocument(): TexNode {
    val result = parseSequence(stop = null)
    skipWhitespace()
    if (position != source.length) fail("unexpected '${source[position]}'")
    return result
}

internal fun TexCursor.parseSequence(stop: Char?): TexNode {
    val children = mutableListOf<TexNode>()
    while (canParseSequence(stop)) children += parseScriptedAtom()
    if (stop != null && !atStop(stop)) fail("unbalanced group")
    return TexNode.Sequence(children)
}

internal fun TexCursor.canParseSequence(stop: Char?): Boolean {
    skipWhitespace()
    return when {
        position >= source.length -> false
        source[position] == '}' && stop != '}' -> fail("unbalanced group")
        else -> source[position] != stop && !source.startsWith("\\right", position)
    }
}

internal fun TexCursor.atStop(stop: Char): Boolean = position < source.length && source[position] == stop

internal fun TexCursor.parseScriptedAtom(): TexNode {
    val atom = parseAtom()
    val scripts = parseScripts()
    return scripts?.let { TexNode.Scripts(atom, it.subscript, it.superscript) } ?: atom
}

internal fun TexCursor.parseScripts(): ScriptPair? {
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

internal fun TexCursor.hasScript(): Boolean =
    position < source.length &&
        (source[position] == '_' || source[position] == '^')

internal fun TexCursor.parseAtom(): TexNode {
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

internal fun TexCursor.parseGroup(): TexNode {
    position++
    val group = parseSequence('}')
    if (position >= source.length || source[position] != '}') fail("unbalanced group")
    position++
    return group
}

internal fun TexCursor.parseScript(): TexNode {
    skipWhitespace()
    if (position >= source.length) fail("missing script")
    return if (source[position] == '{') parseGroup() else parseAtom()
}
