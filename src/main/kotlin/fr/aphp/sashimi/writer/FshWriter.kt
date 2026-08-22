package fr.aphp.sashimi.writer

import jakarta.enterprise.context.ApplicationScoped
import org.hl7.fhir.r4.model.*

/**
 * Sérialise un [StructureDefinition] Logical (kind=LOGICAL, derivation=SPECIALIZATION)
 * en syntaxe FSH (FHIR Shorthand) — la seule forme produite par [fr.aphp.sashimi.mapper.StructureDefinitionMapper].
 *
 * Supporte :
 *  - Logical models
 *  - Invariants (root et element)
 *  - Flags : MS, ?!, SU
 *  - maxLength, comment, definition
 *  - Extensions SQL Sashimi
 *  - Extensions génériques simples et complexes (nested)
 *  - Types multiples (value[x]), Reference, canonical avec profils multiples
 */
@ApplicationScoped
class FshWriter {

  // ── Constantes SQL Sashimi ─────────────────────────────────────────────

  companion object {
    const val EXT_CHARACTERISTICS = "http://hl7.org/fhir/StructureDefinition/structuredefinition-type-characteristics"
  }

  // Modèle de données pour un invariant collecté
  private data class FshInvariant(
    val key: String,
    val severity: String,
    val human: String?,
    val expression: String?,
    val description: String? = null,
  )

  // ── Point d'entrée ────────────────────────────────────────────────────

  fun write(sd: StructureDefinition): String = buildString {
    appendLine(HEADER)
    append(renderSd(sd))
  }

  // ── Header ────────────────────────────────────────────────────────────

  private val HEADER = """
        // ============================================================
        // Généré automatiquement par Sashimi — ne pas modifier manuellement
        // ============================================================
    """.trimIndent()

  // ── StructureDefinition ───────────────────────────────────────────────

  private fun renderSd(sd: StructureDefinition): String {
    // Buffer d'invariants collectés pendant le rendu du corps
    val invariants = mutableListOf<FshInvariant>()

    // Pour un Logical, le path racine est sd.name (ex. "Encounter")
    // Pour un Profile ou Extension, c'est sd.type (ex. "Patient", "Observation")
    val rootPath = when {
      sd.kind == StructureDefinition.StructureDefinitionKind.LOGICAL -> sd.name
      else -> sd.type
        ?: sd.baseDefinition?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: sd.name
    }

    val body = buildString {
      appendLine("Logical: ${sd.name}")

      sd.baseDefinition?.takeIf { it.isNotBlank() }
        ?.let { appendLine("Parent: $it") }

      sd.id?.takeIf { it.isNotBlank() }
        ?.let { appendLine("Id: ${it.lowercase()}") }

      sd.title?.takeIf { it.isNotBlank() }
        ?.let { appendLine("Title: \"${it.fsh()}\"") }

      sd.description?.takeIf { it.isNotBlank() }
        ?.let { appendLine("Description: ${it.fshQuoted()}") }

      // Characteristics (Logical models uniquement)
      val chars = sd.extension.filter { it.url == EXT_CHARACTERISTICS }
      if (chars.isNotEmpty()) {
        appendLine("Characteristics: ${chars.joinToString(" ") { "#${it.value}" }}")
      }

      appendLine()

      val allElements = sd.differential?.element.orEmpty()
      val rootEl = allElements.firstOrNull()
      val elements = allElements.drop(1)

      // Invariants du root → obeys sur le profil lui-même
      rootEl?.constraint.orEmpty().forEach { c ->
        invariants += c.toFshInvariant()
        appendLine("* obeys ${c.key}")
      }
      appendLine()

      elements.forEach { el ->
        append(renderElement(el, rootPath, invariants))
      }

      appendLine()
    }

    // Déclarations top-level émises APRÈS le bloc Profile/Logical
    val invariantBlocks = invariants.joinToString("\n") { renderInvariantBlock(it) }

    return buildString {
      append(body)
      if (invariantBlocks.isNotBlank()) {
        appendLine()
        append(invariantBlocks)
      }
    }
  }

  // ── Invariants ────────────────────────────────────────────────────────

  // Conversion du modèle HAPI → modèle FSH neutre
  private fun ElementDefinition.ElementDefinitionConstraintComponent.toFshInvariant() =
    FshInvariant(
      key        = key,
      severity   = severity?.toCode() ?: "error",
      human      = human?.takeIf { it.isNotBlank() },
      expression = expression?.takeIf { it.isNotBlank() },
    )

  // Rendu du bloc top-level
  private fun renderInvariantBlock(inv: FshInvariant): String = buildString {
    appendLine("Invariant: ${inv.key}")
    inv.human?.let      { appendLine("Description: \"${it.fsh()}\"") }
    inv.expression?.let { appendLine("Expression: \"${it.fsh()}\"") }
    appendLine("Severity: #${inv.severity}")
    appendLine()
  }

  // ── Élément ───────────────────────────────────────────────────────────

  private fun renderElement(
    el: ElementDefinition,
    rootPath: String,
    invariants: MutableList<FshInvariant>): String = buildString {
    val fshPath = el.path.removePrefix("$rootPath.")

    // ── Ligne principale : cardinalité + type + flags + short ───────────
    val parts = mutableListOf("* $fshPath")

    if (el.hasMin() || el.hasMax()) parts += "${el.min}..${el.max ?: "*"}"

    val typeStr = buildTypeString(el)
    if (typeStr.isNotBlank()) parts += typeStr

    val flags = buildInlineFlags(el)
    if (flags.isNotBlank()) parts += flags

    parts += el.short?.takeIf { it.isNotBlank() }?.let { "\"${it.fsh()}\"" } ?: "\"\""
    appendLine(parts.joinToString(" "))

    // ── maxLength ─────────────────────────────────────────────────────
    el.maxLengthElement?.value?.takeIf { it > 0 }
      ?.let { appendLine("* $fshPath ^maxLength = $it") }

    // ── Extensions ────────────────────────────────────────────────────
    el.extension.forEach { ext ->
      append(renderExtension(ext, fshPath))
    }

    // ── Métadonnées textuelles ────────────────────────────────────────
    el.definition?.takeIf { it.isNotBlank() && it != el.short }
      ?.let { appendLine("* $fshPath ^definition = ${it.fshQuoted()}") }

    el.comment?.takeIf { it.isNotBlank() }
      ?.let { appendLine("* $fshPath ^comment = ${it.fshQuoted()}") }

    el.requirements?.takeIf { it.isNotBlank() }
      ?.let { appendLine("* $fshPath ^requirements = ${it.fshQuoted()}") }

    // obeys inline + collecte
    el.constraint.forEach { c ->
      invariants += c.toFshInvariant()
      appendLine("* $fshPath obeys ${c.key}")
    }
  }

  // ── Flags ─────────────────────────────────────────────────────────────

  /**
   * Flags FSH inline : MS (mustSupport), ?! (isModifier), SU (isSummary),
   * TU (experimental / Trial Use), N (normative).
   * On retourne une chaîne vide s'il n'y a rien.
   */
  private fun buildInlineFlags(el: ElementDefinition): String {
    val flags = buildList {
      if (el.mustSupport)  add("MS")
      if (el.isModifier)   add("?!")
      if (el.isSummary)    add("SU")
    }
    return flags.joinToString(" ")
  }

  // ── Types ─────────────────────────────────────────────────────────────

  private fun buildTypeString(el: ElementDefinition): String {
    if (el.type.isEmpty()) return ""
    return el.type.joinToString(" or ") { renderTypeRef(it) }
  }

  private fun renderTypeRef(t: ElementDefinition.TypeRefComponent): String {
    val code = t.code ?: return "string"
    return when {
      code == "Reference" && t.targetProfile.isNotEmpty() ->
        "Reference(${t.targetProfile.joinToString(" or ") { it.value }})"

      code == "canonical" && t.targetProfile.isNotEmpty() ->
        "canonical(${t.targetProfile.joinToString(" or ") { it.value }})"

      // Profil appliqué (DataType contraint)
      t.profile.isNotEmpty() -> t.profile.first().value

      else -> code
    }
  }

  // ── Extensions ────────────────────────────────────────────────────────

  private fun renderExtension(ext: Extension, rel: String): String = buildString {
    appendExtSlice(ext, basePath = "* $rel ^extension")
  }

  // ── Valeurs FHIR → FSH ────────────────────────────────────────────────

  private fun renderValue(t: Type): String = when (t) {
    is BooleanType       -> t.booleanValue().toString()
    is UnsignedIntType   -> t.value.toString()
    is PositiveIntType   -> t.value.toString()
    is IntegerType       -> t.value.toString()
    is DecimalType       -> t.value.toPlainString()
    is MarkdownType      -> "\"${t.value.fsh()}\""
    is CodeType          -> "#${t.value}"
    is IdType            -> "\"${t.value}\""
    is StringType        -> "\"${t.value.fsh()}\""
    is UrlType           -> "\"${t.value}\""
    is CanonicalType     -> "\"${t.value}\""
    is OidType           -> "\"${t.value}\""
    is UuidType          -> "\"${t.value}\""
    is UriType           -> "\"${t.value}\""
    is Base64BinaryType  -> "\"${t.valueAsString}\""
    is DateType          -> "\"${t.valueAsString}\""
    is DateTimeType      -> "\"${t.valueAsString}\""
    is TimeType          -> "\"${t.valueAsString}\""
    is InstantType       -> "\"${t.valueAsString}\""
    is Coding            -> renderCoding(t)
    is CodeableConcept   -> t.coding.firstOrNull()?.let { renderCoding(it) }
      ?: "\"${t.text?.fsh()}\""
    is Quantity          -> renderQuantity(t)
    else                 -> "\"${t.primitiveValue() ?: t.fhirType()}\""
  }

  private fun renderCoding(c: Coding): String =
    if (!c.system.isNullOrBlank()) "${c.system}#${c.code ?: ""}"
    else "#${c.code ?: ""}"

  private fun renderQuantity(q: Quantity): String = buildString {
    append(q.value?.toPlainString() ?: "")
    q.unit?.let { append(" '${it}'") }
    q.system?.let { append(" // system: $it") }
    q.code?.let { append(" // code: $it") }
  }

  // ── Utilitaires String ────────────────────────────────────────────────

  /** Échappe les guillemets et remplace les retours à la ligne. */
  private fun String.fsh() =
    replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\r\n", " ")
      .replace("\n", " ")
      .replace("\r", " ")

  /** Choisit la syntaxe triple-guillemets si la chaîne contient des sauts de ligne. */
  private fun String.fshQuoted(): String =
    if (contains('\n')) "\"\"\"${this}\"\"\"" else "\"${this.fsh()}\""

  private fun String.capitalize() = replaceFirstChar { it.uppercaseChar() }

  // ── Utilitaires StringBuilder ────────────────────────────────────────────────

  private fun StringBuilder.appendExtSlice(ext: Extension, basePath: String) {
    // Ouvre un nouveau slot dans le tableau FSH
    appendLine("$basePath[+].url = \"${ext.url}\"")

    // Toutes les références suivantes à ce slot utilisent [=]
    val sliceRef = "$basePath[=]"

    when {
      ext.hasValue() ->
        appendLine(
          "$sliceRef.value${ext.value.fhirType().capitalize()} = ${renderValue(ext.value)}"
        )

      // Récursion : chaque sub-extension descend d'un niveau dans le chemin
      ext.hasExtension() ->
        ext.extension.forEach { sub ->
          appendExtSlice(sub, basePath = "$sliceRef.extension")
        }
    }
  }
}