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
 * Parse un fichier SQL (DDL) avec le parser jOOQ et retourne les [SqlTable]
 * qu'il décrit — voir `CONTEXT.md` et le ticket #3 de la carte wayfinder #1.
 *
 * Toute la traversée du QOM jOOQ (`$tableElements()`, `$condition()`…) se
 * fait ici, une seule fois : [fr.aphp.sashimi.mapper.StructureDefinitionMapper]
 * ne travaille plus que sur ce modèle de domaine.
 *
 * @param sql         Contenu SQL à parser (une ou plusieurs instructions séparées par `;`).
 * @param dialectName Nom du dialecte jOOQ (insensible à la casse). Valeur par défaut : `"DEFAULT"`.
 * @return Liste des [SqlTable] décrites par les `CREATE TABLE` du fichier, ou liste vide en cas d'erreur de parsing.
 */
@ApplicationScoped
class SqlTableParser {

    private val log = LoggerFactory.getLogger(SqlTableParser::class.java)

    // Rendu dialecte-fidèle du texte des conditions CHECK : voir InvariantText,
    // dont le fix TRUNC(...) dépend spécifiquement du rendu POSTGRES.
    private val postgres = DSL.using(SQLDialect.POSTGRES)

    fun parse(sql: String, dialectName: String = "DEFAULT"): List<SqlTable> {
        val dialect = runCatching { SQLDialect.valueOf(dialectName.uppercase()) }
            .getOrElse {
                log.warn("Dialecte '$dialectName' inconnu, utilisation de DEFAULT")
                SQLDialect.DEFAULT
            }

        val queries = try {
            DSL.using(dialect).parser().parse(sql).`$queries`()
        } catch (e: Exception) {
            log.error("Erreur de parsing SQL : ${e.message}")
            return emptyList()
        }

        val comments = queries.filterIsInstance<QOM.CommentOn>()
        return queries.filterIsInstance<QOM.CreateTable>()
            .map { toSqlTable(it, comments) }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun toSqlTable(ct: QOM.CreateTable, comments: List<QOM.CommentOn>): SqlTable {
        val table = ct.`$table`()
        val elements = ct.`$tableElements`()
        val fields = elements.filterIsInstance<TableField<*, *>>()
        val checkConstraints = elements.filterIsInstance<QOM.Check>()

        val notNullColumns: Set<String> = checkConstraints
            .map { it.`$condition`() }
            .filterIsInstance<IsNotNull>()
            .map { it.`$field`().name }
            .toSet()

        val primaryKeyColumns: Set<String> = elements.filterIsInstance<QOM.PrimaryKey>()
            .flatMap { pk -> pk.`$fields`().map { it.name } }
            .toSet()

        val foreignKeys: List<SqlForeignKey> = elements.filterIsInstance<QOM.ForeignKey>()
            .map { fk ->
                SqlForeignKey(
                    localColumns = fk.`$fields`().map { it.name },
                    targetTable = fk.`$referencesTable`().qualifiedName.unquotedName().toString(),
                    targetColumns = fk.`$referencesFields`().map { it.unqualifiedName.unquotedName().toString() },
                )
            }

        val uniqueKeys: List<SqlUniqueKey> = elements.filterIsInstance<QOM.UniqueKey>()
            .map { uk -> SqlUniqueKey(name = uk.name, columns = uk.`$fields`().map { it.name }) }

        val checks: List<SqlCheckConstraint> = checkConstraints.map { check ->
            SqlCheckConstraint(
                name = check.`$name`().last(),
                conditionText = postgres.render(check.`$condition`()),
            )
        }

        val columns: List<SqlColumn> = fields.map { field ->
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

    private fun tableComment(table: Table<*>, comments: List<QOM.CommentOn>): String? =
        comments.firstOrNull { comment -> comment.`$table`()?.qualifiedName == table.qualifiedName }
            ?.`$comment`()?.comment?.takeIf { it.isNotBlank() }

    private fun fieldComment(table: Table<*>, field: TableField<*, *>, comments: List<QOM.CommentOn>): String? =
        comments.firstOrNull { comment ->
            comment.`$field`()?.qualifiedName?.toString() == "${table.qualifiedName}.${field.qualifiedName}"
        }?.`$comment`()?.comment?.takeIf { it.isNotBlank() }
}
