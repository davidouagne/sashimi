package fr.aphp.sashimi

/**
 * URL of the standard FHIR extension structuredefinition-type-characteristics,
 * shared between [fr.aphp.sashimi.mapper.StructureDefinitionMapper] (which emits
 * it) and [fr.aphp.sashimi.writer.FshWriter] (which recognizes it for the FSH
 * `Characteristics:` keyword).
 */
const val EXT_CHARACTERISTICS = "http://hl7.org/fhir/StructureDefinition/structuredefinition-type-characteristics"
