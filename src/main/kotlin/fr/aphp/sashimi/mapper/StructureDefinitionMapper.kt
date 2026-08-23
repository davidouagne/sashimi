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

/** Base commune aux échecs de validation détectés pendant le mapping (voir #11, #14). */
abstract class MappingValidationException(message: String) : IllegalStateException(message)

/** Levée quand une table SQL produit deux contraintes CHECK dont la clé FHIR résolue est identique. */
class DuplicateConstraintKeyException(message: String) : MappingValidationException(message)

/** Levée quand deux tables SQL d'un même run produisent le même identifiant de StructureDefinition. */
class DuplicateStructureDefinitionIdException(message: String) : MappingValidationException(message)

/** Levée quand une table SQL produit deux Unique Keys dont le nom (nommé ou de repli) résolu est identique. */
class DuplicateUniqueKeyNameException(message: String) : MappingValidationException(message)

/** Une table qui n'a pas pu être mappée : son [SqlTable.name] et la cause de l'échec (voir #16). */
data class TableMappingFailure(val tableName: String, val exception: MappingValidationException)

/**
 * Résultat d'un [StructureDefinitionMapper.map] : succès partiel autorisé (#16) — une table en
 * échec n'empêche pas les autres tables du même run d'être mappées et écrites.
 */
data class MappingResult(val successes: List<StructureDefinition>, val failures: List<TableMappingFailure>)

/**
 * Transforme chaque [SqlTable] (déjà entièrement résolu par
 * [fr.aphp.sashimi.parser.SqlTableParser]) en [StructureDefinition] HAPI FHIR R4.
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
    const val BASE_URL       = "https://interop.aphp.fr/fhir/StructureDefinition"
    const val EXT_IS_PK      = "$BASE_URL/ext-sql-is-pk"
    const val EXT_FK_COLUMNS = "$BASE_URL/ext-sql-fk-columns"
    const val EXT_SQL_UNIQUE = "$BASE_URL/ext-sql-unique"
    const val EXT_PRECISION  = "$BASE_URL/ext-sql-precision"
  }

  /**
   * Mappe chaque table indépendamment : une table en échec (contrainte CHECK/UNIQUE ambiguë, ou
   * collision de sdId avec une autre table) n'empêche pas les autres tables du run d'être mappées
   * (succès partiel, cf. ticket #16). Une collision de sdId entre deux tables par ailleurs valides
   * exclut les deux (ni l'une ni l'autre n'est plus "en faute" que l'autre), pas seulement l'une des deux.
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

    // Deux tables ne doivent jamais produire le même identifiant de StructureDefinition :
    // le second fichier .fsh écraserait silencieusement le premier (cf. ticket #14).
    val mappedPairs = mappedTables.zip(mappedDefinitions)
    val firstTableNameById = mutableMapOf<String, String>()
    val collisionExceptionByTableName = mutableMapOf<String, DuplicateStructureDefinitionIdException>()
    mappedPairs.forEach { (table, sd) ->
      val previousTableName = firstTableNameById.putIfAbsent(sd.id, table.name)
      if (previousTableName != null) {
        val exception = DuplicateStructureDefinitionIdException(
          "Les tables '$previousTableName' et '${table.name}' produisent le même identifiant FHIR " +
            "'${sd.id}' — renommez l'une d'elles pour lever l'ambiguïté."
        )
        // putIfAbsent (pas +=) : une même table peut apparaître comme "previous" pour plusieurs
        // collisions successives (groupe à 3+ tables) ou coïncider avec elle-même (deux tables
        // strictement homonymes) — un seul TableMappingFailure par nom de table, pas un doublon.
        collisionExceptionByTableName.putIfAbsent(previousTableName, exception)
        collisionExceptionByTableName.putIfAbsent(table.name, exception)
      }
    }
    collisionExceptionByTableName.forEach { (tableName, exception) ->
      failures += TableMappingFailure(tableName, exception)
    }

    val successes = mappedPairs.mapNotNull { (table, sd) ->
      sd.takeIf { table.name !in collisionExceptionByTableName }
    }

    return MappingResult(successes, failures)
  }

  // ─────────────────────────────────────────────────────────────────────────

  private fun mapTable(table: SqlTable): StructureDefinition {
    val sdName = table.name.toPascalCase()
    val sdId = table.name.toKebabCase()

    // Deux UNIQUE (nommées et/ou anonymes) ne doivent jamais résoudre au même nom : ce serait deux
    // contraintes distinctes rendues indistinguables dans le FSH généré (même pattern qu'en #11
    // pour les clés CHECK, appliqué ici aux Unique Keys — cf. ticket #15).
    val seenUniqueKeyNames = mutableSetOf<String>()
    table.uniqueKeys.forEach { uniqueKey ->
      val name = uniqueKeyName(uniqueKey)
      if (!seenUniqueKeyNames.add(name)) {
        throw DuplicateUniqueKeyNameException(
          "Table '${table.name}' : plusieurs contraintes UNIQUE produisent le même nom " +
            "'$name' — renommez-les explicitement pour lever l'ambiguïté."
        )
      }
    }

    // ── StructureDefinition ───────────────────────────────────────────────
    val sd = StructureDefinition().apply {
      url            = "$BASE_URL/$sdName"
      baseDefinition = "Base"
      id             = sdId
      name           = sdName
      title          = table.name
      status         = PublicationStatus.DRAFT
      kind           = StructureDefinitionKind.LOGICAL
      abstract       = false
      type           = sdName
      derivation     = TypeDerivationRule.SPECIALIZATION
      description    = table.comment
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

      var anonymousCheckIndex = 0
      val seenCheckKeys = mutableSetOf<String>()
      table.checks.forEach { check ->
        val normalizedCondition = InvariantText.normalize(check.conditionText)
        // Repli anonyme basé sur le contenu (stable face au réordonnancement de la DDL, cf. ticket #12) ;
        // l'index positionnel ne sert plus qu'en tout dernier recours si la condition normalisée
        // elle-même se réduit à rien (cf. ticket #13).
        val key = check.name?.toConstraintKey()?.takeIf { it.isNotBlank() }
          ?: normalizedCondition.toConstraintKey().takeIf { it.isNotBlank() }?.let { "chk-$it" }
          ?: run {
            anonymousCheckIndex++
            "chk-${sdName.lowercase()}-$anonymousCheckIndex"
          }

        // Deux CHECK (nommées et/ou anonymes) ne doivent jamais produire la même clé FHIR :
        // ce serait deux invariants distincts silencieusement fusionnés en un seul (cf. ticket #11).
        if (!seenCheckKeys.add(key)) {
          throw DuplicateConstraintKeyException(
            "Table '${table.name}' : plusieurs contraintes CHECK produisent la même clé FHIR " +
              "'$key' — renommez-les explicitement pour lever l'ambiguïté."
          )
        }

        addConstraint(ElementDefinition.ElementDefinitionConstraintComponent().apply {
          this.key    = key
          severity    = ElementDefinition.ConstraintSeverity.ERROR
          human       = normalizedCondition
          expression  = "true"
        })
      }
    }
    sd.differential.addElement(rootEl)

    // Un ElementDefinition par colonne, contexte pré-calculé en une passe
    buildColumnContexts(table).forEach { context ->
      sd.differential.addElement(buildElement(context, sdName))
    }
    return sd
  }

  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Une FK à laquelle participe une colonne locale donnée, avec la colonne cible qui lui
   * correspond *positionnellement* (voir ticket #10 : `localColumns[i]` ↔ `targetColumns[i]`,
   * pas le produit cartésien des deux listes).
   */
  private data class ColumnForeignKey(
    val fk: SqlForeignKey,
    val targetColumn: String,
  )

  /** Tous les faits d'une colonne (cardinalité, PK, FK, unique) résolus en une seule passe sur [SqlTable]. */
  private data class ColumnContext(
    val column: SqlColumn,
    val min: Int,
    val max: String,
    val isPrimaryKey: Boolean,
    val foreignKeys: List<ColumnForeignKey>,
    val uniqueKey: SqlUniqueKey?,
  )

  private fun buildColumnContexts(table: SqlTable): List<ColumnContext> =
    table.columns.map { column ->
      val forcedNotNull = column.name in table.notNullColumns
      ColumnContext(
        column       = column,
        min          = if (forcedNotNull || !column.nullable) 1 else 0,
        max          = "1",
        isPrimaryKey = column.name in table.primaryKeyColumns,
        foreignKeys  = table.foreignKeys.mapNotNull { fk ->
          val index = fk.localColumns.indexOf(column.name)
          if (index < 0) null else ColumnForeignKey(fk, fk.targetColumns[index])
        },
        uniqueKey    = table.uniqueKeys.firstOrNull { column.name in it.columns },
      )
    }

  private fun buildElement(context: ColumnContext, parentPath: String): ElementDefinition {
    val column = context.column
    val elementPath = "$parentPath.${column.name.toCamelCase()}"

    return ElementDefinition().apply {
      id   = elementPath
      path = elementPath
      min  = context.min
      max  = context.max

      // ── Type : une Reference vers la SD cible par FK à laquelle la colonne participe,
      //    sinon type FHIR primitif si la colonne n'est dans aucune FK ──
      if (context.foreignKeys.isNotEmpty()) {
        context.foreignKeys.forEach { columnFk -> addType(buildFkTypeRef(columnFk.fk)) }
      } else {
        addType(TypeRefComponent().apply {
          code = sqlTypeToFhirType(column.sqlType)
        })
      }

      column.comment?.let { short = it }

      column.length.takeIf { it > 0 }?.let { len ->
        maxLengthElement = IntegerType(len)
      }

      column.precision.takeIf { it > 0 }?.let { precision ->
        addExtension(Extension().apply {
          url = EXT_PRECISION
          setValue(StringType("($precision${if (column.scale > 0) ",${column.scale}" else ""})"))
        })
      }

      if (context.isPrimaryKey) {
        addExtension(Extension().apply {
          url = EXT_IS_PK
          setValue(BooleanType("true"))
        })
      }

      context.foreignKeys.forEach { columnFk -> addExtension(buildFkColumnsExtension(columnFk)) }

      if (context.uniqueKey != null) {
        addExtension(Extension().apply {
          url = EXT_SQL_UNIQUE
          setValue(StringType("${uniqueKeyName(context.uniqueKey)} [UNIQUE]"))
        })
      }
    }
  }

  /** Nom d'une UNIQUE (verbatim si nommée), ou repli basé sur ses colonnes si anonyme (ex. code -> uq-code). */
  private fun uniqueKeyName(uniqueKey: SqlUniqueKey): String =
    uniqueKey.name
      ?: "uq-" + uniqueKey.columns.joinToString("-") { it.toKebabCase() }

  /**
   * Construit le [TypeRefComponent] Reference pointant vers la SD cible de la FK.
   *
   * Exemple : FK vers `OS_KERN.PATIENT` → `Reference(OsKernPatient)`
   */
  private fun buildFkTypeRef(fk: SqlForeignKey): TypeRefComponent {
    val targetSdName = fk.targetTable.toPascalCase()
    return TypeRefComponent().apply {
      code = "Reference"
      addTargetProfile(targetSdName)
    }
  }

  /**
   * Construit l'extension [EXT_FK_COLUMNS] pour une FK à laquelle participe la colonne locale :
   * une seule sous-extension `targetColumn`, la colonne cible positionnellement correspondante
   * (voir ticket #10 — pas le produit cartésien de toutes les colonnes cibles de la FK).
   */
  private fun buildFkColumnsExtension(columnFk: ColumnForeignKey): Extension =
    Extension().apply {
      url = EXT_FK_COLUMNS
      addExtension(Extension().apply {
        url = "targetColumn"
        setValue(StringType(columnFk.targetColumn.toCamelCase()))
      })
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
