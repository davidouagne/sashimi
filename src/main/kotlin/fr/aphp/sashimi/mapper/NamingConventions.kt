package fr.aphp.sashimi.mapper

// ── Segmentation ──────────────────────────────────────────────────────────

/** Découpe sur tout séparateur (_  .  -  espace) et met en minuscules. */
internal fun String.toWords(): List<String> =
    lowercase()
        .split(Regex("[._\\-\\s]+"))
        .filter { it.isNotBlank() }

// ── Conventions de nommage ────────────────────────────────────────────────

/** care_site → CareSite */
internal fun String.toPascalCase() =
    toWords().joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

/** START_DATE → startDate, note → note */
internal fun String.toCamelCase() =
    toWords()
        .mapIndexed { i, part ->
            if (i == 0) part else part.replaceFirstChar { it.uppercaseChar() }
        }
        .joinToString("")

/** care_site → care-site */
internal fun String.toKebabCase() =
    toWords().joinToString("-")
