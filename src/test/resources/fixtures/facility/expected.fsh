// ============================================================
// Généré automatiquement par Sashimi — ne pas modifier manuellement
// ============================================================
Logical: Facility
Parent: Base
Id: facility
Title: "FACILITY"
Characteristics: #can-be-target


* id 1..1 uuid ""
* id ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-is-pk"
* id ^extension[=].valueBoolean = true
* countryCode 1..1 Reference(Region) ""
* countryCode ^maxLength = 2
* countryCode ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* countryCode ^extension[=].extension[+].url = "targetColumn"
* countryCode ^extension[=].extension[=].valueString = "countryCode"
* regionCode 1..1 Reference(Region) ""
* regionCode ^maxLength = 10
* regionCode ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* regionCode ^extension[=].extension[+].url = "targetColumn"
* regionCode ^extension[=].extension[=].valueString = "regionCode"
* responsibleId 0..1 Reference(Person) or Reference(Organization) ""
* responsibleId ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* responsibleId ^extension[=].extension[+].url = "targetColumn"
* responsibleId ^extension[=].extension[=].valueString = "id"
* responsibleId ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* responsibleId ^extension[=].extension[+].url = "targetColumn"
* responsibleId ^extension[=].extension[=].valueString = "id"

