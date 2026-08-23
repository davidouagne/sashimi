// ============================================================
// Généré automatiquement par Sashimi — ne pas modifier manuellement
// ============================================================
Logical: AnonymousConstraints
Parent: Base
Id: anonymous-constraints
Title: "ANONYMOUS_CONSTRAINTS"
Characteristics: #can-be-target

* obeys chk-a-0
* obeys chk-b-0
* obeys chk-id-is-not-null

* id 1..1 uuid ""
* id ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-is-pk"
* id ^extension[=].valueBoolean = true
* code 0..1 string ""
* code ^maxLength = 10
* code ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-unique"
* code ^extension[=].valueString = "uq-code [UNIQUE]"
* a 0..1 integer ""
* a ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* a ^extension[=].valueString = "(10)"
* a ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-unique"
* a ^extension[=].valueString = "uq-a-b [UNIQUE]"
* b 0..1 integer ""
* b ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* b ^extension[=].valueString = "(10)"
* b ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-unique"
* b ^extension[=].valueString = "uq-a-b [UNIQUE]"


Invariant: chk-a-0
Description: "a > 0"
Expression: "true"
Severity: #error


Invariant: chk-b-0
Description: "b > 0"
Expression: "true"
Severity: #error


Invariant: chk-id-is-not-null
Description: "id IS NOT NULL"
Expression: "true"
Severity: #error

