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

* id 1..1 BackboneElement ""
* id.value 1..1 uuid ""
* id.isPrimaryKey 1..1 boolean "Primary key member"
* code 0..1 BackboneElement ""
* code.value 0..1 string ""
* code.value ^maxLength = 10
* code.uniqueKeyName 1..1 string "Unique key name"
* a 0..1 BackboneElement ""
* a.value 0..1 integer ""
* a.uniqueKeyName 1..1 string "Unique key name"
* a.precision 1..1 integer "Numeric precision"
* b 0..1 BackboneElement ""
* b.value 0..1 integer ""
* b.uniqueKeyName 1..1 string "Unique key name"
* b.precision 1..1 integer "Numeric precision"


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

