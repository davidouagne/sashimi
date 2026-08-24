package fr.aphp.sashimi.parser

/**
 * Domain model for the parser/mapper seam (see wayfinder map #1, ticket #3):
 * what [SqlTableParser] builds from the DDL, fully resolved — the mapper never
 * depends on `org.jooq.impl.QOM.*` again. Detailed vocabulary in `CONTEXT.md`
 * (SQL Table, SQL Column, Not-Null-Forcing Column…).
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

/** Pure DDL fact: [nullable] is never merged with [SqlTable.notNullColumns] here. */
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
    val name: String?,
    val columns: List<String>,
)

/**
 * [conditionText] is the raw, dialect-faithful jOOQ rendering of the CHECK
 * condition (before FSH normalization — whitespace/parentheses/camelCase,
 * done by [fr.aphp.sashimi.mapper.InvariantText] on the mapper side).
 */
data class SqlCheckConstraint(
    val name: String?,
    val conditionText: String,
)
