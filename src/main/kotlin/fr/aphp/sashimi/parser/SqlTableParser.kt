package fr.aphp.sashimi.parser

import jakarta.enterprise.context.ApplicationScoped
import org.jooq.SQLDialect
import org.jooq.Table
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.QOM
import org.jooq.impl.QOM.IsNotNull
import org.slf4j.LoggerFactory

/**
 * Parses a SQL (DDL) file with the jOOQ parser and returns the [SqlTable]s
 * it describes — see `CONTEXT.md` and ticket #3 of wayfinder map #1.
 *
 * All traversal of jOOQ's QOM (`$tableElements()`, `$condition()`…) happens
 * here, once: [fr.aphp.sashimi.mapper.StructureDefinitionMapper] only ever
 * works on this domain model afterwards.
 *
 * @param sql         SQL content to parse (one or more statements separated by `;`).
 * @param dialectName jOOQ dialect name (case-insensitive). Default value: `"DEFAULT"`.
 * @return List of [SqlTable]s described by the file's `CREATE TABLE` statements, or an empty list on a parsing error.
 */
@ApplicationScoped
class SqlTableParser {
    private val log = LoggerFactory.getLogger(SqlTableParser::class.java)

    // Dialect-faithful rendering of CHECK condition text: see InvariantText,
    // whose TRUNC(...) fix specifically depends on the POSTGRES rendering.
    private val postgres = DSL.using(SQLDialect.POSTGRES)

    fun parse(
        sql: String,
        dialectName: String = "DEFAULT",
    ): List<SqlTable> {
        val dialect =
            runCatching { SQLDialect.valueOf(dialectName.uppercase()) }
                .getOrElse {
                    log.warn("Unknown dialect '$dialectName', falling back to DEFAULT")
                    SQLDialect.DEFAULT
                }

        val queries =
            try {
                DSL
                    .using(dialect)
                    .parser()
                    .parse(sql)
                    .`$queries`()
            } catch (e: Exception) {
                log.error("SQL parsing error: ${e.message}")
                return emptyList()
            }

        val comments = queries.filterIsInstance<QOM.CommentOn>()
        return queries
            .filterIsInstance<QOM.CreateTable>()
            .map { toSqlTable(it, comments) }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun toSqlTable(
        ct: QOM.CreateTable,
        comments: List<QOM.CommentOn>,
    ): SqlTable {
        val table = ct.`$table`()
        val elements = ct.`$tableElements`()
        val fields = elements.filterIsInstance<TableField<*, *>>()
        val checkConstraints = elements.filterIsInstance<QOM.Check>()

        val notNullColumns: Set<String> =
            checkConstraints
                .map { it.`$condition`() }
                .filterIsInstance<IsNotNull>()
                .map { it.`$field`().name }
                .toSet()

        val primaryKeyColumns: Set<String> =
            elements
                .filterIsInstance<QOM.PrimaryKey>()
                .flatMap { pk -> pk.`$fields`().map { it.name } }
                .toSet()

        val foreignKeys: List<SqlForeignKey> =
            elements
                .filterIsInstance<QOM.ForeignKey>()
                .map { fk ->
                    SqlForeignKey(
                        localColumns = fk.`$fields`().map { it.name },
                        targetTable =
                            fk
                                .`$referencesTable`()
                                .qualifiedName
                                .unquotedName()
                                .toString(),
                        targetColumns = fk.`$referencesFields`().map { it.unqualifiedName.unquotedName().toString() },
                    )
                }

        val uniqueKeys: List<SqlUniqueKey> =
            elements
                .filterIsInstance<QOM.UniqueKey>()
                .map { uk ->
                    SqlUniqueKey(
                        name = uk.name.takeIf { it.isNotBlank() },
                        columns = uk.`$fields`().map { it.name },
                    )
                }

        val checks: List<SqlCheckConstraint> =
            checkConstraints.map { check ->
                SqlCheckConstraint(
                    // check.$name().last() returns "" (not null) for an anonymous CHECK
                    name = check.`$name`().last()?.takeIf { it.isNotBlank() },
                    conditionText = postgres.render(check.`$condition`()),
                )
            }

        val columns: List<SqlColumn> =
            fields.map { field ->
                SqlColumn(
                    name = field.name,
                    sqlType = field.dataType.typeName.uppercase(),
                    length = field.dataType.length(),
                    precision = field.dataType.precision(),
                    scale = field.dataType.scale(),
                    nullable = field.dataType.nullability().nullable(),
                    comment = fieldComment(table, field, comments),
                )
            }

        return SqlTable(
            name = table.qualifiedName.unquotedName().toString(),
            comment = tableComment(table, comments),
            columns = columns,
            primaryKeyColumns = primaryKeyColumns,
            foreignKeys = foreignKeys,
            uniqueKeys = uniqueKeys,
            checks = checks,
            notNullColumns = notNullColumns,
        )
    }

    private fun tableComment(
        table: Table<*>,
        comments: List<QOM.CommentOn>,
    ): String? =
        comments
            .firstOrNull { comment -> comment.`$table`()?.qualifiedName == table.qualifiedName }
            ?.`$comment`()
            ?.comment
            ?.takeIf { it.isNotBlank() }

    private fun fieldComment(
        table: Table<*>,
        field: TableField<*, *>,
        comments: List<QOM.CommentOn>,
    ): String? =
        comments
            .firstOrNull { comment ->
                comment.`$field`()?.qualifiedName?.toString() == "${table.qualifiedName}.${field.qualifiedName}"
            }?.`$comment`()
            ?.comment
            ?.takeIf { it.isNotBlank() }
}
