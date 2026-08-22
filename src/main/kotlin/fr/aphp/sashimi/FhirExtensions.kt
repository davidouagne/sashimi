package fr.aphp.sashimi

/**
 * URL de l'extension FHIR standard structuredefinition-type-characteristics,
 * partagée entre [fr.aphp.sashimi.mapper.StructureDefinitionMapper] (qui l'émet)
 * et [fr.aphp.sashimi.writer.FshWriter] (qui la reconnaît pour le mot-clé
 * FSH `Characteristics:`).
 */
const val EXT_CHARACTERISTICS = "http://hl7.org/fhir/StructureDefinition/structuredefinition-type-characteristics"
