package fr.aphp.sashimi.writer

import jakarta.enterprise.context.ApplicationScoped
import org.hl7.fhir.r4.model.*

/**
 * Sérialise n'importe quelle [StructureDefinition] en syntaxe FSH (FHIR Shorthand).
 *
 * Supporte :
 *  - Logical, Profile, Extension, Resource
 *  - Slicing + slices nommées
 *  - Bindings (required / extensible / preferred / example)
 *  - Fixed value et pattern (tous les types primitifs courants + Coding, CodeableConcept)
 *  - Invariants (root et element)
 *  - Flags : MS, ?!, SU, TU, N
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

  private sealed class RenderMode {

    /** Logical / Resource : définition complète de chaque élément */
    data object Definition : RenderMode()

    /**
     * Profile / Extension : on n'émet que ce qui est contraint.
     * Le type n'est émis que s'il est réellement restreint (Reference profilée,
     * type narrow) ou si l'élément est un slice nommé.
     */
    data object Constraint : RenderMode()
  }

  // Modèle de données pour un invariant collecté
  private data class FshInvariant(
    val key: String,
    val severity: String,
    val human: String?,
    val expression: String?,
    val description: String? = null,
  )

  private data class SliceEntry(
    val name: String,
    val cardinality: String,
    val flags: String?,
  ) {
    fun toContainsToken() = listOfNotNull(name, cardinality, flags).joinToString(" ")
  }

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
    val mode = renderModeFor(sd)
    // Buffer d'invariants collectés pendant le rendu du corps
    val invariants = mutableListOf<FshInvariant>()

    // Évite d'émettre la clause contains plusieurs fois si le path apparaît N fois
    val emittedContains = mutableSetOf<String>()

    // Pour un Logical, le path racine est sd.name (ex. "Encounter")
    // Pour un Profile ou Extension, c'est sd.type (ex. "Patient", "Observation")
    val rootPath = when {
      sd.kind == StructureDefinition.StructureDefinitionKind.LOGICAL -> sd.name
      else -> sd.type
        ?: sd.baseDefinition?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: sd.name
    }

    val body = buildString {
      // Keyword FSH selon le kind + derivation
      val keyword = sdKeyword(sd)
      appendLine("$keyword: ${sd.name}")

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

      // Pour chaque path slicé → liste ordonnée des sliceNames
      val sliceNamesByPath: Map<String, List<SliceEntry>> = elements
        .filter { !it.sliceName.isNullOrBlank() }
        .groupBy { it.path }
        .mapValues { (_, els) ->
          els.map { el ->
            SliceEntry(
              name        = el.sliceName!!,
              cardinality = if (el.hasMin() || el.hasMax()) "${el.min}..${el.max ?: "*"}" else "0..*",
              flags       = buildInlineFlags(el).takeIf { it.isNotBlank() },
            )
          }
        }

      // Invariants du root → obeys sur le profil lui-même
      rootEl?.constraint.orEmpty().forEach { c ->
        invariants += c.toFshInvariant()
        appendLine("* obeys ${c.key}")
      }
      appendLine()

      elements.forEach { el ->
        append(renderElement(el, rootPath, mode, invariants, sliceNamesByPath, emittedContains))
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

  private fun renderModeFor(sd: StructureDefinition) = when (sd.derivation) {
    StructureDefinition.TypeDerivationRule.SPECIALIZATION -> RenderMode.Definition
    StructureDefinition.TypeDerivationRule.CONSTRAINT     -> RenderMode.Constraint
    else                                                  -> RenderMode.Definition
  }

  // ── Keyword FSH ───────────────────────────────────────────────────────

  private fun sdKeyword(sd: StructureDefinition): String = when {
    sd.kind == StructureDefinition.StructureDefinitionKind.LOGICAL -> "Logical"
    sd.type == "Extension" -> "Extension"
    sd.derivation == StructureDefinition.TypeDerivationRule.CONSTRAINT -> "Profile"
    sd.kind == StructureDefinition.StructureDefinitionKind.RESOURCE -> "Resource"
    else -> "Profile"
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
    mode: RenderMode,
    invariants: MutableList<FshInvariant>,
    sliceNamesByPath: Map<String, List<SliceEntry>>,
    emittedContains: MutableSet<String>): String = buildString {
    val rel = el.path.removePrefix("$rootPath.")
    val isSlice  = !el.sliceName.isNullOrBlank()
    // Substitution du chemin : identifier:INS → identifier[INS]
    val fshPath = if (!el.sliceName.isNullOrBlank()) "$rel[${el.sliceName}]" else rel

    // ── Ligne principale ──────────────────────────────────────────────────
    val parts = mutableListOf("* $fshPath")

    when (mode) {
      RenderMode.Definition -> {
        // Définition complète : cardinalité + type toujours émis
        if (!isSlice && (el.hasMin() || el.hasMax())) parts += "${el.min}..${el.max ?: "*"}"

        val typeStr = buildTypeString(el)
        if (typeStr.isNotBlank()) parts += typeStr

        if (!isSlice) {
          val flags = buildInlineFlags(el)
          if (flags.isNotBlank()) parts += flags
        }

        parts += el.short?.takeIf { it.isNotBlank() }?.let { "\"${it.fsh()}\"" } ?: "\"\""
        appendLine(parts.joinToString(" "))
      }
      RenderMode.Constraint -> {
        // Contrainte : cardinalité seulement si présente dans le differential
        if (!isSlice && (el.hasMin() || el.hasMax())) parts += "${el.min}..${el.max ?: "*"}"

        if (!isSlice) {
          // Flags toujours pertinents en mode contrainte
          val flags = buildInlineFlags(el)
          if (flags.isNotBlank()) parts += flags
          appendLine(parts.joinToString(" "))
        }

        el.short?.takeIf { it.isNotBlank() }
          ?.let { appendLine("* $fshPath ^short = ${it.fshQuoted()}") }
      }
    }

    // ── Slicing : règles puis contains ───────────────────────────────────
    el.slicing?.let { slicing ->
      append(renderSlicing(slicing, fshPath))

      if (el.path !in emittedContains) {
        sliceNamesByPath[el.path]?.let { entries ->
          val tokens = entries.joinToString(" and ") { it.toContainsToken() }
          appendLine("* $fshPath contains $tokens")
        }
        emittedContains += el.path
      }
    }

    // ── Binding ───────────────────────────────────────────────────────
    el.binding?.takeIf { it.hasValueSet() }?.let { b ->
      val strength = b.strength?.toCode() ?: "required"
      appendLine("* $fshPath from ${b.valueSet} ($strength)")
    }

    // ── Fixed value ───────────────────────────────────────────────────
    el.fixed?.let { appendLine("* $fshPath = ${renderValue(it)}") }

    // ── Pattern ───────────────────────────────────────────────────────
    el.pattern?.let {
      appendLine("* $fshPath ^pattern${it.fhirType().capitalize()} = ${renderValue(it)}")
    }

    // ── maxLength ─────────────────────────────────────────────────────
    el.maxLengthElement?.value?.takeIf { it > 0 }
      ?.let { appendLine("* $fshPath ^maxLength = $it") }

    // ── Extensions ────────────────────────────────────────────────────
    el.extension.forEach { ext ->
      append(renderExtension(ext, rel))
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

  /**
   * Renvoie true si le type de l'élément apporte réellement une contrainte
   * par rapport à la base : Reference avec profil cible, canonical profilé,
   * ou type narrowé (liste de types plus courte / différente).
   * Un type code seul ("string", "boolean"…) sans profil n'est pas une contrainte.
   */
  private fun isConstrainedType(el: ElementDefinition): Boolean =
    el.type.any { t ->
      t.profile.isNotEmpty() ||
        t.targetProfile.isNotEmpty() ||
        el.type.size > 1   // type[x] narrowé explicitement
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

  // ── Slicing ───────────────────────────────────────────────────────────

  private fun renderSlicing(
    s: ElementDefinition.ElementDefinitionSlicingComponent,
    rel: String,
  ): String = buildString {
    s.discriminator.forEachIndexed { i, d ->
      val idx = if (i == 0) "[+]" else "[=]"
      appendLine("* $rel ^slicing.discriminator$idx.type = #${d.type?.toCode()}")
      appendLine("* $rel ^slicing.discriminator[=].path = \"${d.path}\"")
    }
    s.description?.takeIf { it.isNotBlank() }
      ?.let { appendLine("* $rel ^slicing.description = \"${it.fsh()}\"") }
    if (s.ordered) appendLine("* $rel ^slicing.ordered = true")
    s.rules?.let { appendLine("* $rel ^slicing.rules = #${it.toCode()}") }
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