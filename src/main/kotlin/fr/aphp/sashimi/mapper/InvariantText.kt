package fr.aphp.sashimi.mapper

/**
 * Normalise le texte brut d'une condition SQL CHECK (déjà rendu dialecte-fidèle
 * par [fr.aphp.sashimi.parser.SqlTableParser], voir [fr.aphp.sashimi.parser.SqlCheckConstraint.conditionText])
 * en texte d'invariant FSH lisible.
 */
internal object InvariantText {

    private val SQL_KEYWORDS = setOf("is", "not", "null", "or", "and", "in", "like", "between")

    private val TOKEN = Regex("""'(?:[^']|'')*'|\b[a-zA-Z][a-zA-Z0-9_]*\b""")

    fun normalize(rawConditionText: String): String =
        rawConditionText
            .normalizeWhitespace()
            .normalizeParentheses()
            .applySqlNamingConventions()

    private fun String.normalizeWhitespace() =
        replace(Regex("""\s+"""), " ").trim()

    private fun String.normalizeParentheses() =
        replace(Regex("""\(\s+"""), "(")
            .replace(Regex("""\s+\)"""), ")")

    /**
     * Convertit les identifiants SQL snake_case en camelCase, sans toucher
     * aux mots-clés SQL, aux littéraux entre quotes (ex. 'J', 'B'), ni aux
     * noms de fonction SQL suivis d'une parenthèse (ex. date_trunc(...)).
     */
    private fun String.applySqlNamingConventions(): String {
        val source = this
        return TOKEN.replace(source) { match ->
            val token = match.value
            when {
                token.startsWith("'")                        -> token
                token.lowercase() in SQL_KEYWORDS            -> token.uppercase()
                isFollowedByOpenParen(source, match.range.last) -> token.lowercase()
                else                                          -> token.lowercase().toCamelCase()
            }
        }
    }

    private fun isFollowedByOpenParen(source: String, lastMatchedIndex: Int): Boolean {
        var i = lastMatchedIndex + 1
        while (i < source.length && source[i].isWhitespace()) i++
        return i < source.length && source[i] == '('
    }
}
