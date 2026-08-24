package fr.aphp.sashimi.mapper

import fr.aphp.sashimi.EXT_CHARACTERISTICS
import fr.aphp.sashimi.parser.SqlColumn
import fr.aphp.sashimi.parser.SqlForeignKey
import fr.aphp.sashimi.parser.SqlTable
import fr.aphp.sashimi.parser.SqlUniqueKey
import jakarta.enterprise.context.ApplicationScoped
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.ElementDefinition
import org.hl7.fhir.r4.model.ElementDefinition.TypeRefComponent
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind
import org.hl7.fhir.r4.model.StructureDefinition.TypeDerivationRule
import org.hl7.fhir.r4.model.Type

/** Common base for validation failures detected during mapping (see #11, #14). */
abstract class MappingValidationException(
    message: String,
) : IllegalStateException(message)

/** Thrown when a SQL table produces two CHECK constraints whose resolved FHIR key is identical. */
class DuplicateConstraintKeyException(
    message: String,
) : MappingValidationException(message)

/** Thrown when two SQL tables in the same run produce the same StructureDefinition id. */
class DuplicateStructureDefinitionIdException(
    message: String,
) : MappingValidationException(message)

/** Thrown when a SQL table produces two Unique Keys whose resolved name (explicit or fallback) is identical. */
class DuplicateUniqueKeyNameException(
    message: String,
) : MappingValidationException(message)

/** A table that could not be mapped: its [SqlTable.name] and the cause of the failure (see #16). */
data class TableMappingFailure(
    val tableName: String,
    val exception: MappingValidationException,
)

/**
 * Result of a [StructureDefinitionMapper.map] call: partial success is allowed (#16) — a failing
 * table does not prevent the other tables in the same run from being mapped and written.
 */
data class MappingResult(
    val successes: List<StructureDefinition>,
    val failures: List<TableMappingFailure>,
)

/**
 * Transforms each [SqlTable] (already fully resolved by
 * [fr.aphp.sashimi.parser.SqlTableParser]) into a HAPI FHIR R4 [StructureDefinition].
 *
 * | DDL source          | FHIR target                                                          |
 * |----------------------|----------------------------------------------------------------------|
 * | Column + type         | ElementDefinition with a primitive FHIR type (column with no fact)  |
 * | Column + facts        | BackboneElement ElementDefinition, children `.value`/`.isPrimaryKey`/`.uniqueKeyName`/`.precision`/`.scale`/`.fkN.*` (see `docs/backbone-element-migration-spec.md`) |
 * | NOT NULL              | min = 1                                                               |
 * | NULL                  | min = 0                                                               |
 * | PRIMARY KEY            | BackboneElement child `.isPrimaryKey: boolean`                       |
 * | FOREIGN KEY            | BackboneElement group `.fkN.reference: Reference(target SD)` + `.fkN.targetColumn: string` |
 * | UNIQUE KEY             | BackboneElement child `.uniqueKeyName: string`                       |
 * | CHECK IS NOT NULL      | reinforces cardinality to min = 1                                    |
 * | Precision/scale        | BackboneElement children `.precision: integer` / `.scale: integer`  |
 */
@ApplicationScoped
class StructureDefinitionMapper {
    companion object {
        const val BASE_URL = "https://interop.aphp.fr/fhir/StructureDefinition"
    }

    /**
     * Maps each table independently: a failing table (ambiguous CHECK/UNIQUE constraint, or an
     * sdId collision with another table) does not prevent the other tables in the run from being
     * mapped (partial success, see ticket #16). An sdId collision between two otherwise-valid
     * tables excludes both (neither is more "at fault" than the other), not just one of the two.
     */
    fun map(tables: List<SqlTable>): MappingResult {
        val failures = mutableListOf<TableMappingFailure>()
        val mappedTables = mutableListOf<SqlTable>()
        val mappedDefinitions = mutableListOf<StructureDefinition>()
        tables.forEach { table ->
            try {
                mappedDefinitions += mapTable(table)
                mappedTables += table
            } catch (e: MappingValidationException) {
                failures += TableMappingFailure(table.name, e)
            }
        }

        // Two tables must never produce the same StructureDefinition id: the second .fsh file
        // would silently overwrite the first (see ticket #14).
        val mappedPairs = mappedTables.zip(mappedDefinitions)
        val firstTableNameById = mutableMapOf<String, String>()
        val collisionExceptionByTableName = mutableMapOf<String, DuplicateStructureDefinitionIdException>()
        mappedPairs.forEach { (table, sd) ->
            val previousTableName = firstTableNameById.putIfAbsent(sd.id, table.name)
            if (previousTableName != null) {
                val exception =
                    DuplicateStructureDefinitionIdException(
                        "Tables '$previousTableName' and '${table.name}' produce the same FHIR identifier " +
                            "'${sd.id}' — rename one of them to resolve the ambiguity.",
                    )
                // putIfAbsent (not +=): the same table can appear as "previous" for several
                // successive collisions (a group of 3+ tables) or coincide with itself (two
                // strictly identically-named tables) — one TableMappingFailure per table name, not a duplicate.
                collisionExceptionByTableName.putIfAbsent(previousTableName, exception)
                collisionExceptionByTableName.putIfAbsent(table.name, exception)
            }
        }
        collisionExceptionByTableName.forEach { (tableName, exception) ->
            failures += TableMappingFailure(tableName, exception)
        }

        val successes =
            mappedPairs.mapNotNull { (table, sd) ->
                sd.takeIf { table.name !in collisionExceptionByTableName }
            }

        return MappingResult(successes, failures)
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun mapTable(table: SqlTable): StructureDefinition {
        val sdName = table.name.toPascalCase()
        val sdId = table.name.toKebabCase()

        // Two UNIQUE constraints (named and/or anonymous) must never resolve to the same name: that
        // would be two distinct constraints rendered indistinguishable in the generated FSH (same
        // pattern as #11 for CHECK keys, applied here to Unique Keys — see ticket #15).
        val seenUniqueKeyNames = mutableSetOf<String>()
        table.uniqueKeys.forEach { uniqueKey ->
            val name = uniqueKeyName(uniqueKey)
            seenUniqueKeyNames.addOrThrow(name) {
                DuplicateUniqueKeyNameException(
                    "Table '${table.name}': several UNIQUE constraints produce the same name " +
                        "'$name' — rename them explicitly to resolve the ambiguity.",
                )
            }
        }

        // ── StructureDefinition ───────────────────────────────────────────────
        val sd =
            StructureDefinition().apply {
                url = "$BASE_URL/$sdName"
                baseDefinition = "Base"
                id = sdId
                name = sdName
                title = table.name
                status = PublicationStatus.DRAFT
                kind = StructureDefinitionKind.LOGICAL
                abstract = false
                type = sdName
                derivation = TypeDerivationRule.SPECIALIZATION
                description = table.comment
            }

        sd.addExtension(
            Extension().apply {
                url = EXT_CHARACTERISTICS
                value = CodeType("can-be-target")
            },
        )

        // Root element + CHECK invariants
        val rootEl =
            ElementDefinition().apply {
                id = sdName
                path = sdName
                min = 0
                max = "*"
                addType(TypeRefComponent().apply { code = "Base" })

                var anonymousCheckIndex = 0
                val seenCheckKeys = mutableSetOf<String>()
                table.checks.forEach { check ->
                    val normalizedCondition = InvariantText.normalize(check.conditionText)
                    // Content-based anonymous fallback (stable against DDL reordering, see ticket #12);
                    // the positional index is now only a last resort, if the normalized condition
                    // itself reduces to nothing (see ticket #13).
                    val key =
                        check.name?.toConstraintKey()?.takeIf { it.isNotBlank() }
                            ?: normalizedCondition.toConstraintKey().takeIf { it.isNotBlank() }?.let { "chk-$it" }
                            ?: run {
                                anonymousCheckIndex++
                                "chk-${sdName.lowercase()}-$anonymousCheckIndex"
                            }

                    // Two CHECK constraints (named and/or anonymous) must never produce the same FHIR
                    // key: that would be two distinct invariants silently merged into one (see ticket #11).
                    seenCheckKeys.addOrThrow(key) {
                        DuplicateConstraintKeyException(
                            "Table '${table.name}': several CHECK constraints produce the same FHIR key " +
                                "'$key' — rename them explicitly to resolve the ambiguity.",
                        )
                    }

                    addConstraint(
                        ElementDefinition.ElementDefinitionConstraintComponent().apply {
                            this.key = key
                            severity = ElementDefinition.ConstraintSeverity.ERROR
                            human = normalizedCondition
                            expression = "true"
                        },
                    )
                }
            }
        sd.differential.addElement(rootEl)

        // One or more ElementDefinitions per column (BackboneElement + children if the column
        // carries at least one fact), context precomputed in a single pass
        buildColumnContexts(table).forEach { context ->
            buildColumnElements(context, sdName).forEach { sd.differential.addElement(it) }
        }
        return sd
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** All facts about a column (cardinality, PK, FK, unique) resolved in a single pass over [SqlTable]. */
    private data class ColumnContext(
        val column: SqlColumn,
        val min: Int,
        val max: String,
        val isPrimaryKey: Boolean,
        val foreignKeys: List<SqlForeignKey>,
        val uniqueKey: SqlUniqueKey?,
    )

    private fun buildColumnContexts(table: SqlTable): List<ColumnContext> =
        table.columns.map { column ->
            val forcedNotNull = column.name in table.notNullColumns
            ColumnContext(
                column = column,
                min = if (forcedNotNull || !column.nullable) 1 else 0,
                max = "1",
                isPrimaryKey = column.name in table.primaryKeyColumns,
                // The positionally-corresponding target column (see ticket #10) is no longer carried
                // here: since the BackboneElement migration (#20/#21), `.fkN.targetColumn` is declared
                // with no fixed value (see `docs/backbone-element-migration-spec.md`), so it's no longer read.
                foreignKeys = table.foreignKeys.filter { column.name in it.localColumns },
                uniqueKey = table.uniqueKeys.firstOrNull { column.name in it.columns },
            )
        }

    /**
     * One or more [ElementDefinition]s for a column: a single primitive element if it carries no
     * structural fact, otherwise a `BackboneElement` wrapper followed by its children (in the
     * order `.value`, `.isPrimaryKey`, `.uniqueKeyName`, `.precision`, `.scale`, `.fkN.*` — see
     * `docs/backbone-element-migration-spec.md`, resolutions #20/#21 of wayfinder map #18).
     */
    private fun buildColumnElements(
        context: ColumnContext,
        parentPath: String,
    ): List<ElementDefinition> {
        val column = context.column
        val elementPath = "$parentPath.${column.name.toCamelCase()}"
        val hasFacts =
            context.isPrimaryKey ||
                context.foreignKeys.isNotEmpty() ||
                context.uniqueKey != null ||
                column.precision > 0

        if (!hasFacts) return listOf(buildPrimitiveElement(context, elementPath))

        val elements = mutableListOf<ElementDefinition>()

        elements +=
            ElementDefinition().apply {
                id = elementPath
                path = elementPath
                min = context.min
                max = context.max
                addType(TypeRefComponent().apply { code = "BackboneElement" })
            }

        // `.value` carries the original scalar value, in the same form as an unwrapped primitive
        // element — omitted on an FK column, whose `.fkN.reference`(s) already play that role
        // (see resolution #21).
        if (context.foreignKeys.isEmpty()) {
            elements += buildPrimitiveElement(context, "$elementPath.value")
        }

        // Every fact below is known at mapping time (resolved from the DDL) and constant for every
        // row of the column: it is fixed via an Assignment Rule, on top of its declaration — same
        // logic as the former extension (`^extension[=].value... = ...`), not just its shape. Only
        // `.value` and `.fkN.reference` remain without a fixed value: those are per-record data,
        // not schema facts.
        if (context.isPrimaryKey) {
            elements += leafElement("$elementPath.isPrimaryKey", 1, "1", "boolean", "Primary key member", BooleanType(true))
        }

        context.uniqueKey?.let {
            elements += leafElement("$elementPath.uniqueKeyName", 1, "1", "string", "Unique key name", StringType(uniqueKeyName(it)))
        }

        if (column.precision > 0) {
            elements += leafElement("$elementPath.precision", 1, "1", "integer", "Numeric precision", IntegerType(column.precision))
        }

        if (column.scale > 0) {
            elements += leafElement("$elementPath.scale", 0, "1", "integer", "Numeric scale", IntegerType(column.scale))
        }

        context.foreignKeys.forEachIndexed { index, fk ->
            val fkPath = "$elementPath.fk${index + 1}"
            val targetSdName = fk.targetTable.toPascalCase()
            // Target column *positionally* matching the local column (see ticket #10:
            // localColumns[i] <-> targetColumns[i], not the cartesian product of the two lists).
            val targetColumnName = fk.targetColumns[fk.localColumns.indexOf(column.name)].toCamelCase()

            elements +=
                ElementDefinition().apply {
                    id = fkPath
                    path = fkPath
                    min = 1
                    max = "1"
                    addType(TypeRefComponent().apply { code = "BackboneElement" })
                    short = "Foreign key to $targetSdName"
                }
            elements +=
                ElementDefinition().apply {
                    id = "$fkPath.reference"
                    path = "$fkPath.reference"
                    min = 1
                    max = "1"
                    addType(
                        TypeRefComponent().apply {
                            code = "Reference"
                            addTargetProfile(targetSdName)
                        },
                    )
                }
            elements += leafElement("$fkPath.targetColumn", 1, "1", "string", null, StringType(targetColumnName))
        }

        return elements
    }

    private fun buildPrimitiveElement(
        context: ColumnContext,
        elementPath: String,
    ): ElementDefinition {
        val column = context.column
        return ElementDefinition().apply {
            id = elementPath
            path = elementPath
            min = context.min
            max = context.max
            addType(TypeRefComponent().apply { code = sqlTypeToFhirType(column.sqlType) })
            column.comment?.let { short = it }
            column.length.takeIf { it > 0 }?.let { len -> maxLengthElement = IntegerType(len) }
        }
    }

    private fun leafElement(
        path: String,
        min: Int,
        max: String,
        typeCode: String,
        short: String?,
        fixed: Type? = null,
    ): ElementDefinition =
        ElementDefinition().apply {
            id = path
            this.path = path
            this.min = min
            this.max = max
            addType(TypeRefComponent().apply { code = typeCode })
            short?.let { this.short = it }
            fixed?.let { this.fixed = it }
        }

    /** Name of a UNIQUE constraint (verbatim if named), or a fallback based on its columns if anonymous (e.g. code -> uq-code). */
    private fun uniqueKeyName(uniqueKey: SqlUniqueKey): String =
        uniqueKey.name
            ?: "uq-" + uniqueKey.columns.joinToString("-") { it.toKebabCase() }

    // ─────────────────────────────────────────────────────────────────────────

    private fun sqlTypeToFhirType(sqlType: String): String =
        when (sqlType.substringBefore("(").trim()) {
            "VARCHAR", "VARCHAR2", "NVARCHAR", "NVARCHAR2",
            "CHAR", "NCHAR", "CLOB", "NCLOB", "TEXT",
            "LONG", "XMLTYPE",
            -> "string"

            "BOOLEAN", "BOOL" -> "boolean"

            "NUMBER", "NUMERIC", "DECIMAL" -> "decimal"

            "INTEGER", "INT", "INT4", "SMALLINT",
            "TINYINT", "INT2",
            "BINARY_INTEGER", "PLS_INTEGER",
            -> "integer"

            "BIGINT", "INT8" -> "integer64"

            "FLOAT", "REAL", "DOUBLE",
            "FLOAT8", "BINARY_FLOAT", "BINARY_DOUBLE",
            -> "decimal"

            "DATE" -> "date"
            "TIMESTAMP" -> "dateTime"
            "TIME", "INTERVAL" -> "string"

            "UUID", "GUID", "RAW" -> "uuid"
            "BLOB", "BYTEA", "BINARY",
            "VARBINARY", "LONG RAW",
            -> "base64Binary"

            "JSON", "JSONB" -> "string"

            else -> "string"
        }

    // ─────────────────────────────────────────────────────────────────────────

    /** chk_note CHECK → chk-note-check  (valid FSH key) */
    private fun String.toConstraintKey() =
        lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    /**
     * Adds [key] to this set of keys already seen within the same table, or throws the exception
     * from [exception] if it's already there — a pattern shared by CHECK keys (#11) and UNIQUE
     * names (#15) within a table (see ticket #17; the sdId collision between tables, #14/#16, no
     * longer throws since the partial success added in #16, and so no longer follows this same shape).
     */
    private fun MutableSet<String>.addOrThrow(
        key: String,
        exception: () -> MappingValidationException,
    ) {
        if (!add(key)) throw exception()
    }
}
