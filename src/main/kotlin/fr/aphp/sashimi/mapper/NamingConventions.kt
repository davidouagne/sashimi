package fr.aphp.sashimi.mapper

// ── Segmentation ──────────────────────────────────────────────────────────

/** Splits on any separator (_  .  -  space) and lowercases. */
internal fun String.toWords(): List<String> =
    lowercase()
        .split(Regex("[._\\-\\s]+"))
        .filter { it.isNotBlank() }

// ── Naming conventions ────────────────────────────────────────────────────

/** care_site → CareSite */
internal fun String.toPascalCase() = toWords().joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

/** START_DATE → startDate, note → note */
internal fun String.toCamelCase() =
    toWords()
        .mapIndexed { i, part ->
            if (i == 0) part else part.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")

/** care_site → care-site */
internal fun String.toKebabCase() = toWords().joinToString("-")
