// ============================================================
// Généré automatiquement par Sashimi — ne pas modifier manuellement
// ============================================================
Logical: PatientRecord
Parent: Base
Id: patient-record
Title: "PATIENT_RECORD"
Characteristics: #can-be-target


* id 1..1 BackboneElement ""
* id.value 1..1 uuid ""
* id.isPrimaryKey 1..1 boolean "Primary key member"
* id.isPrimaryKey = true
* ipp 1..1 string ""
* ipp ^maxLength = 20
* lastName 1..1 string ""
* lastName ^maxLength = 255
* firstName 1..1 string ""
* firstName ^maxLength = 255
* birthDate 0..1 date ""
* gender 0..1 string ""
* gender ^maxLength = 10
* active 1..1 boolean ""
* createdAt 1..1 dateTime ""
* updatedAt 0..1 dateTime ""

