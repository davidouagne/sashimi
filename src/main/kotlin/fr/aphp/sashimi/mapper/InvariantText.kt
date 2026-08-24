package fr.aphp.sashimi.mapper

/**
 * Normalizes the raw text of a SQL CHECK condition (already rendered
 * dialect-faithfully by [fr.aphp.sashimi.parser.SqlTableParser], see
 * [fr.aphp.sashimi.parser.SqlCheckConstraint.conditionText]) into readable
 * FSH invariant text.
 */
internal object InvariantText {
    private val SQL_KEYWORDS = setOf("is", "not", "null", "or", "and", "in", "like", "between")

    private val TOKEN = Regex("""'(?:[^']|'')*'|\b[a-zA-Z][a-zA-Z0-9_]*\b""")

    fun normalize(rawConditionText: String): String =
        rawConditionText
            .normalizeWhitespace()
            .normalizeParentheses()
            .applySqlNamingConventions()

    private fun String.normalizeWhitespace() = replace(Regex("""\s+"""), " ").trim()

    private fun String.normalizeParentheses() =
        replace(Regex("""\(\s+"""), "(")
            .replace(Regex("""\s+\)"""), ")")

    /**
     * Converts snake_case SQL identifiers to camelCase, without touching
     * SQL keywords, quoted literals (e.g. 'J', 'B'), or SQL function names
     * followed by a parenthesis (e.g. date_trunc(...)).
     */
    private fun String.applySqlNamingConventions(): String {
        val source = this
        return TOKEN.replace(source) { match ->
            val token = match.value
            when {
                token.startsWith("'") -> token
                token.lowercase() in SQL_KEYWORDS -> token.uppercase()
                isFollowedByOpenParen(source, match.range.last) -> token.lowercase()
                else -> token.lowercase().toCamelCase()
            }
        }
    }

    private fun isFollowedByOpenParen(
        source: String,
        lastMatchedIndex: Int,
    ): Boolean {
        var i = lastMatchedIndex + 1
        while (i < source.length && source[i].isWhitespace()) i++
        return i < source.length && source[i] == '('
    }
}
