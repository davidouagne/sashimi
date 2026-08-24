// ============================================================
// Généré automatiquement par Sashimi — ne pas modifier manuellement
// ============================================================
Logical: Encounter
Parent: Base
Id: encounter
Title: "ENCOUNTER"
Characteristics: #can-be-target


* id 1..1 BackboneElement ""
* id.value 1..1 uuid ""
* id.isPrimaryKey 1..1 boolean "Primary key member"
* patientId 1..1 uuid ""
* encounterDate 1..1 dateTime ""
* dischargeDate 0..1 dateTime ""
* status 1..1 string ""
* status ^maxLength = 50
* note 0..1 string ""

