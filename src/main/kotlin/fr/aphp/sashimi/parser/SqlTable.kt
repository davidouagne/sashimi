package fr.aphp.sashimi.parser

/**
 * Modèle de domaine du seam parser/mapper (voir carte wayfinder #1, ticket #3) :
 * ce que [SqlTableParser] construit à partir du DDL, entièrement résolu — le
 * mapper ne dépend plus jamais de `org.jooq.impl.QOM.*`. Vocabulaire détaillé
 * dans `CONTEXT.md` (SQL Table, SQL Column, Not-Null-Forcing Column…).
 */
data class SqlTable(
    val name: String,
    val comment: String?,
    val columns: List<SqlColumn>,
    val primaryKeyColumns: Set<String>,
    val foreignKeys: List<SqlForeignKey>,
    val uniqueKeys: List<SqlUniqueKey>,
    val checks: List<SqlCheckConstraint>,
    val notNullColumns: Set<String>,
)

/** Fait DDL pur : [nullable] n'est jamais fusionné avec [SqlTable.notNullColumns] ici. */
data class SqlColumn(
    val name: String,
    val sqlType: String,
    val length: Int,
    val precision: Int,
    val scale: Int,
    val nullable: Boolean,
    val comment: String?,
)

data class SqlForeignKey(
    val localColumns: List<String>,
    val targetTable: String,
    val targetColumns: List<String>,
)

data class SqlUniqueKey(
    val name: String,
    val columns: List<String>,
)

/**
 * [conditionText] est le rendu jOOQ dialecte-fidèle brut de la condition CHECK
 * (avant normalisation FSH — whitespace/parenthèses/camelCase, faite par
 * [fr.aphp.sashimi.mapper.InvariantText] côté mapper).
 */
data class SqlCheckConstraint(
    val name: String?,
    val conditionText: String,
)
