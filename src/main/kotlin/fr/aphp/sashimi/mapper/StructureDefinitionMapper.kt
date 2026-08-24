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
import org.hl7.fhir.r4.model.Type
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
 * | Source DDL        | Cible FHIR                                                          |
 * |-------------------|----------------------------------------------------------------------|
 * | Colonne + type    | ElementDefinition avec type FHIR primitif (colonne sans aucun fait)  |
 * | Colonne + faits   | ElementDefinition BackboneElement, enfants `.value`/`.isPrimaryKey`/`.uniqueKeyName`/`.precision`/`.scale`/`.fkN.*` (voir `docs/backbone-element-migration-spec.md`) |
 * | NOT NULL          | min = 1                                                               |
 * | NULL              | min = 0                                                               |
 * | PRIMARY KEY       | enfant BackboneElement `.isPrimaryKey: boolean`                      |
 * | FOREIGN KEY       | groupe BackboneElement `.fkN.reference: Reference(SD cible)` + `.fkN.targetColumn: string` |
 * | UNIQUE KEY        | enfant BackboneElement `.uniqueKeyName: string`                      |
 * | CHECK IS NOT NULL | renforce la cardinalité min = 1                                      |
 * | Précision/échelle | enfants BackboneElement `.precision: integer` / `.scale: integer`    |
 */
@ApplicationScoped
class StructureDefinitionMapper {

  companion object {
    const val BASE_URL = "https://interop.aphp.fr/fhir/StructureDefinition"
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
      seenUniqueKeyNames.addOrThrow(name) {
        DuplicateUniqueKeyNameException(
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
        seenCheckKeys.addOrThrow(key) {
          DuplicateConstraintKeyException(
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

    // Un ou plusieurs ElementDefinition par colonne (BackboneElement + enfants si la colonne
    // porte au moins un fait), contexte pré-calculé en une passe
    buildColumnContexts(table).forEach { context ->
      buildColumnElements(context, sdName).forEach { sd.differential.addElement(it) }
    }
    return sd
  }

  // ─────────────────────────────────────────────────────────────────────────

  /** Tous les faits d'une colonne (cardinalité, PK, FK, unique) résolus en une seule passe sur [SqlTable]. */
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
        column       = column,
        min          = if (forcedNotNull || !column.nullable) 1 else 0,
        max          = "1",
        isPrimaryKey = column.name in table.primaryKeyColumns,
        // La colonne cible positionnellement correspondante (voir ticket #10) n'est plus portée
        // ici : depuis la migration BackboneElement (#20/#21), `.fkN.targetColumn` est déclaré
        // sans valeur fixée (cf. `docs/backbone-element-migration-spec.md`), donc plus lue.
        foreignKeys  = table.foreignKeys.filter { column.name in it.localColumns },
        uniqueKey    = table.uniqueKeys.firstOrNull { column.name in it.columns },
      )
    }

  /**
   * Un ou plusieurs [ElementDefinition] pour une colonne : un seul élément primitif si elle ne
   * porte aucun fait structurel, sinon un wrapper `BackboneElement` suivi de ses enfants (dans
   * l'ordre `.value`, `.isPrimaryKey`, `.uniqueKeyName`, `.precision`, `.scale`, `.fkN.*` — voir
   * `docs/backbone-element-migration-spec.md`, résolutions #20/#21 de la carte wayfinder #18).
   */
  private fun buildColumnElements(context: ColumnContext, parentPath: String): List<ElementDefinition> {
    val column = context.column
    val elementPath = "$parentPath.${column.name.toCamelCase()}"
    val hasFacts = context.isPrimaryKey ||
      context.foreignKeys.isNotEmpty() ||
      context.uniqueKey != null ||
      column.precision > 0

    if (!hasFacts) return listOf(buildPrimitiveElement(context, elementPath))

    val elements = mutableListOf<ElementDefinition>()

    elements += ElementDefinition().apply {
      id   = elementPath
      path = elementPath
      min  = context.min
      max  = context.max
      addType(TypeRefComponent().apply { code = "BackboneElement" })
    }

    // `.value` porte la valeur scalaire d'origine, sous la même forme qu'un élément primitif non
    // enveloppé — omis sur une colonne FK, dont le(s) `.fkN.reference` jouent déjà ce rôle
    // (cf. résolution #21).
    if (context.foreignKeys.isEmpty()) {
      elements += buildPrimitiveElement(context, "$elementPath.value")
    }

    // Chaque fait ci-dessous est connu au moment du mapping (résolu depuis la DDL) et constant
    // pour toutes les lignes de la colonne : on le fixe via une Assignment Rule, en plus de sa
    // déclaration — même logique que l'ancienne extension (`^extension[=].value... = ...`), pas
    // seulement sa forme. Seuls `.value` et `.fkN.reference` restent sans valeur fixée : ce sont
    // des données propres à chaque enregistrement, pas des faits de schéma.
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
      // Colonne cible correspondant *positionnellement* à la colonne locale (voir ticket #10 :
      // localColumns[i] <-> targetColumns[i], pas le produit cartésien des deux listes).
      val targetColumnName = fk.targetColumns[fk.localColumns.indexOf(column.name)].toCamelCase()

      elements += ElementDefinition().apply {
        id    = fkPath
        path  = fkPath
        min   = 1
        max   = "1"
        addType(TypeRefComponent().apply { code = "BackboneElement" })
        short = "Foreign key to $targetSdName"
      }
      elements += ElementDefinition().apply {
        id   = "$fkPath.reference"
        path = "$fkPath.reference"
        min  = 1
        max  = "1"
        addType(TypeRefComponent().apply {
          code = "Reference"
          addTargetProfile(targetSdName)
        })
      }
      elements += leafElement("$fkPath.targetColumn", 1, "1", "string", null, StringType(targetColumnName))
    }

    return elements
  }

  private fun buildPrimitiveElement(context: ColumnContext, elementPath: String): ElementDefinition {
    val column = context.column
    return ElementDefinition().apply {
      id   = elementPath
      path = elementPath
      min  = context.min
      max  = context.max
      addType(TypeRefComponent().apply { code = sqlTypeToFhirType(column.sqlType) })
      column.comment?.let { short = it }
      column.length.takeIf { it > 0 }?.let { len -> maxLengthElement = IntegerType(len) }
    }
  }

  private fun leafElement(
    path: String, min: Int, max: String, typeCode: String, short: String?, fixed: Type? = null,
  ): ElementDefinition =
    ElementDefinition().apply {
      id   = path
      this.path = path
      this.min  = min
      this.max  = max
      addType(TypeRefComponent().apply { code = typeCode })
      short?.let { this.short = it }
      fixed?.let { this.fixed = it }
    }

  /** Nom d'une UNIQUE (verbatim si nommée), ou repli basé sur ses colonnes si anonyme (ex. code -> uq-code). */
  private fun uniqueKeyName(uniqueKey: SqlUniqueKey): String =
    uniqueKey.name
      ?: "uq-" + uniqueKey.columns.joinToString("-") { it.toKebabCase() }

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

  /**
   * Ajoute [key] à cet ensemble de clés déjà vues au sein d'une même table, ou lève l'exception
   * de [exception] si elle y est déjà — pattern partagé par les clés CHECK (#11) et les noms
   * UNIQUE (#15) au sein d'une table (cf. ticket #17 ; la collision de sdId entre tables, #14/#16,
   * ne throw plus depuis le succès partiel du #16 et ne suit donc plus cette même forme).
   */
  private fun MutableSet<String>.addOrThrow(key: String, exception: () -> MappingValidationException) {
    if (!add(key)) throw exception()
  }
}
