package fr.aphp.sashimi.mapper

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
import org.jooq.Query
import org.jooq.Table
import org.jooq.TableField
import org.jooq.impl.QOM
import org.jooq.impl.QOM.IsNotNull

/**
 * Transforme tous les [QOM.CreateTable] d'un fichier DDL en [StructureDefinition] HAPI FHIR R4.
 *
 * Les `COMMENT ON` associés (table ou colonne) sont également consommés
 * pour alimenter [StructureDefinition.description] et [ElementDefinition.short].
 *
 * | Source DDL        | Cible FHIR                                                    |
 * |-------------------|---------------------------------------------------------------|
 * | Colonne + type    | ElementDefinition avec type FHIR primitif                     |
 * | NOT NULL          | min = 1                                                       |
 * | NULL              | min = 0                                                       |
 * | PRIMARY KEY       | extension ext-sql-pk (valeur : true)                          |
 * | FOREIGN KEY       | type = Reference (SD cible) + extension ext-sql-fk-columns     |
 * | UNIQUE KEY        | extension ext-sql-unique sur la colonne concernée             |
 * | CHECK IS NOT NULL | renforce la cardinalité min = 1                               |
 * | Précision/échelle | extension ext-sql-precision "(precision,scale)"               |
 */
@ApplicationScoped
class StructureDefinitionMapper {

  companion object {
    const val BASE_URL            = "https://interop.aphp.fr/fhir/StructureDefinition"
    const val EXT_CHARACTERISTICS = "http://hl7.org/fhir/StructureDefinition/structuredefinition-type-characteristics"
    const val EXT_IS_PK           = "$BASE_URL/ext-sql-is-pk"
    const val EXT_FK_COLUMNS      = "$BASE_URL/ext-sql-fk-columns"
    const val EXT_SQL_UNIQUE      = "$BASE_URL/ext-sql-unique"
    const val EXT_PRECISION       = "$BASE_URL/ext-sql-precision"
  }

  fun map(queries: List<Query>): List<StructureDefinition> =
    queries
      .filterIsInstance<QOM.CreateTable>()
      .map { mapCreateTable(it, queries.filterIsInstance<QOM.CommentOn>()) }

  // ─────────────────────────────────────────────────────────────────────────

  private fun mapCreateTable(ct: QOM.CreateTable, comments: List<QOM.CommentOn>): StructureDefinition {
    val table  = ct.`$table`()
    val sdName = table.qualifiedName.unquotedName().toString().toPascalCase()
    val sdId = table.qualifiedName.unquotedName().toString().toKebabCase()

    val commentByColumn: Map<String, QOM.CommentOn?> = ct.`$tableElements`()
      .filterIsInstance<TableField<*, *>>()
      .associate { field -> field.name to getComment(table, field, comments) }

    val pkFields: Set<String> = ct.`$tableElements`()
      .filterIsInstance<QOM.PrimaryKey>()
      .flatMap { pk -> pk.`$fields`().map { f -> f.name } }
      .toSet()

    val checkConstraints: List<QOM.Check> = ct.`$tableElements`()
      .filterIsInstance<QOM.Check>()
    val cardByColumn: Map<String, Pair<Int, String>> = ct.`$tableElements`()
      .filterIsInstance<TableField<*, *>>()
      .associate { field -> field.name to getCardinality(field, checkConstraints) }

    val foreignKeys: List<QOM.ForeignKey> = ct.`$tableElements`()
      .filterIsInstance<QOM.ForeignKey>()
    val fkByColumn: Map<String, QOM.ForeignKey?> = ct.`$tableElements`()
      .filterIsInstance<TableField<*, *>>()
      .associate { field -> field.name to getForeignKey(field, foreignKeys) }

    val uniqueKeys: List<QOM.UniqueKey> = ct.`$tableElements`()
      .filterIsInstance<QOM.UniqueKey>()
    val uniqueKeyByColumn: Map<String, QOM.UniqueKey?> = ct.`$tableElements`()
      .filterIsInstance<TableField<*, *>>()
      .associate { field -> field.name to getUniqueKey(field, uniqueKeys) }

    // ── StructureDefinition ───────────────────────────────────────────────
    val sd = StructureDefinition().apply {
      url            = "$BASE_URL/$sdName"
      baseDefinition = "Base"
      id             = sdId
      name           = sdName
      title          = table.qualifiedName.unquotedName().toString()
      status         = PublicationStatus.DRAFT
      kind           = StructureDefinitionKind.LOGICAL
      abstract       = false
      type           = sdName
      derivation     = TypeDerivationRule.SPECIALIZATION
      description    = getComment(table, comments)?.`$comment`()?.comment?.takeIf { c -> c.isNotBlank() }
    }

    sd.addExtension(Extension().apply {
      url = EXT_CHARACTERISTICS
      value = CodeType("can-be-target")
    })

    // Élément racine + invariants CHECK
    val rootEl = ElementDefinition().apply {
      id   = sdName
      path = sdName
      min  = 0
      max  = "*"
      addType(TypeRefComponent().apply { code = "Base" })

      checkConstraints.forEach { check ->
        addConstraint(ElementDefinition.ElementDefinitionConstraintComponent().apply {
          key        = check.`$name`().last()?.toConstraintKey()
            ?: "chk-${sdName.lowercase()}"
          severity   = ElementDefinition.ConstraintSeverity.ERROR
          human      = InvariantText.render(check.`$condition`())
          expression = "true"
        })
      }
    }
    sd.differential.addElement(rootEl)

    // Un ElementDefinition par colonne
    ct.`$tableElements`()
      .filterIsInstance<TableField<*, *>>()
      .forEach { field ->
        sd.differential.addElement(
          buildElement(
            field       = field,
            parentPath  = sdName,
            cardinality = cardByColumn[field.name] ?: Pair(0, "1"),
            comment     = commentByColumn[field.name],
            isPk        = field.name in pkFields,
            fk          = fkByColumn[field.name],
            uniqueKey   = uniqueKeyByColumn[field.name]
          )
        )
      }
    return sd
  }

  // ─────────────────────────────────────────────────────────────────────────

  private fun buildElement(
    field: TableField<*, *>,
    parentPath: String?,
    cardinality: Pair<Int, String>,
    comment: QOM.CommentOn?,
    isPk: Boolean,
    fk: QOM.ForeignKey?,
    uniqueKey: QOM.UniqueKey?,
  ): ElementDefinition {
    val elementPath = "$parentPath.${field.unqualifiedName.unquotedName().toString().toCamelCase()}"

    return ElementDefinition().apply {
      id   = elementPath
      path = elementPath
      min  = cardinality.first
      max  = cardinality.second

      // ── Type : Reference vers la SD cible si FK, sinon type FHIR primitif ──
      if (fk != null) {
        addType(buildFkTypeRef(fk))
      } else {
        addType(TypeRefComponent().apply {
          code = sqlTypeToFhirType(field.dataType.typeName.uppercase())
        })
      }

      comment?.`$comment`()?.comment?.takeIf { it.isNotBlank() }?.let { short = it }

      field.dataType.length().takeIf { it > 0 }?.let { len ->
        maxLengthElement = IntegerType(len)
      }

      field.dataType.precision().takeIf { it > 0 }?.let { precision ->
        val scale = field.dataType.scale()
        addExtension(Extension().apply {
          url = EXT_PRECISION
          setValue(StringType("($precision${if (scale > 0) ",$scale" else ""})"))
        })
      }

      if (isPk) {
        addExtension(Extension().apply {
          url = EXT_IS_PK
          setValue(BooleanType("true"))
        })
      }

      if (fk != null) {
        addExtension(buildFkColumnsExtension(fk))
      }

      if (uniqueKey != null) {
        addExtension(Extension().apply {
          url = EXT_SQL_UNIQUE
          setValue(StringType("${uniqueKey.name} [UNIQUE]"))
        })
      }
    }
  }

  /**
   * Construit le [TypeRefComponent] Reference pointant vers la SD cible de la FK.
   *
   * Exemple : FK vers `OS_KERN.PATIENT` → `Reference(https://…/OsKernPatient)`
   */
  private fun buildFkTypeRef(fk: QOM.ForeignKey): TypeRefComponent {
    val targetSdUrl = fk.`$referencesTable`().qualifiedName.unquotedName().toString().toPascalCase()
    return TypeRefComponent().apply {
      code = "Reference"
      addTargetProfile(targetSdUrl)
    }
  }

  /**
   * Construit l'extension [EXT_FK_COLUMNS] avec deux sous-extensions :
   *   - `targetColumn` : nom de la colonne cible dans la table référencée
   *
   * En cas de FK composite (rare, mais possible), une extension est créée
   * par paire locale/cible.
   */
  private fun buildFkColumnsExtension(
    fk: QOM.ForeignKey,
  ): Extension {
    // Retrouve la position de la colonne locale dans la liste des champs FK
    val targetFields = fk.`$referencesFields`()

    return Extension().apply {
      url = EXT_FK_COLUMNS
      // sous-extension targetColumn
      targetFields.forEach { field ->
        addExtension(Extension().apply {
          url = "targetColumn"
          setValue(StringType(field.unqualifiedName.unquotedName().toString().toCamelCase()))
        })
      }
    }
  }

  // ─────────────────────────────────────────────────────────────────────────

  private fun getCardinality(
    field: TableField<*, *>,
    constraints: List<QOM.Check>,
  ): Pair<Int, String> {
    val forcedNotNull = constraints
      .map { it.`$condition`() }
      .filterIsInstance<IsNotNull>()
      .any { it.`$field`().name == field.name }

    return if (forcedNotNull || !field.dataType.nullability().nullable()) {
      Pair(1, "1")
    } else {
      Pair(0, "1")
    }
  }

  private fun getForeignKey(
    field: TableField<*, *>,
    foreignKeys: List<QOM.ForeignKey>?,
  ): QOM.ForeignKey? =
    foreignKeys?.firstOrNull { fk ->
      fk.`$fields`().any { fkCol -> fkCol.name == field.name }
    }

  private fun getUniqueKey(
    field: TableField<*, *>,
    uniqueKeys: List<QOM.UniqueKey>?,
  ): QOM.UniqueKey? =
    uniqueKeys?.firstOrNull { fk ->
      fk.`$fields`().any { fkCol -> fkCol.name == field.name }
    }

  private fun getComment(
    table: Table<*>,
    comments: List<QOM.CommentOn>?,
  ): QOM.CommentOn? =
    comments?.firstOrNull { comment ->
      comment.`$table`()?.let {
        it.qualifiedName == table.qualifiedName
      } == true
    }

  private fun getComment(
    table: Table<*>,
    field: TableField<*, *>,
    comments: List<QOM.CommentOn>?,
  ): QOM.CommentOn? =
    comments?.firstOrNull { comment ->
      comment.`$field`()?.let {
        it.qualifiedName.toString() == "${table.qualifiedName}.${field.qualifiedName}"
      } == true
    }

  // ─────────────────────────────────────────────────────────────────────────

  private fun sqlTypeToFhirType(sqlType: String): String =
    when (sqlType.substringBefore("(").trim()) {
      "VARCHAR", "VARCHAR2", "NVARCHAR", "NVARCHAR2",
      "CHAR", "NCHAR", "CLOB", "NCLOB", "TEXT",
      "LONG", "XMLTYPE"                           -> "string"

      "BOOLEAN", "BOOL"                           -> "boolean"

      "NUMBER", "NUMERIC", "DECIMAL"              -> "decimal"

      "INTEGER", "INT", "INT4", "SMALLINT",
      "TINYINT", "INT2",
      "BINARY_INTEGER", "PLS_INTEGER"             -> "integer"

      "BIGINT", "INT8"                            -> "integer64"

      "FLOAT", "REAL", "DOUBLE",
      "FLOAT8", "BINARY_FLOAT", "BINARY_DOUBLE"   -> "decimal"

      "DATE"                                      -> "date"
      "TIMESTAMP"                                 -> "dateTime"
      "TIME", "INTERVAL"                          -> "string"

      "UUID", "GUID", "RAW"                       -> "uuid"
      "BLOB", "BYTEA", "BINARY",
      "VARBINARY", "LONG RAW"                     -> "base64Binary"

      "JSON", "JSONB"                             -> "string"

      else                                        -> "string"
    }

  // ─────────────────────────────────────────────────────────────────────────

  /** chk_note CHECK → chk-note-check  (clé FSH valide) */
  private fun String.toConstraintKey() =
    lowercase()
      .replace(Regex("[^a-z0-9]+"), "-")
      .trim('-')
}
