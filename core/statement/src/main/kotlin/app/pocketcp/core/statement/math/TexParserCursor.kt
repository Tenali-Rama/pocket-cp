package app.pocketcp.core.statement.math

internal fun TexCursor.skipWhitespace() {
    while (position < source.length && source[position].isWhitespace()) position++
}

internal fun TexCursor.peek(): Char? = source.getOrNull(position)

internal fun TexCursor.fail(message: String): Nothing = throw TexParseException(message)
