// ============================================================
// Généré automatiquement par Sashimi — ne pas modifier manuellement
// ============================================================
Logical: Facility
Parent: Base
Id: facility
Title: "FACILITY"
Characteristics: #can-be-target


* id 1..1 BackboneElement ""
* id.value 1..1 uuid ""
* id.isPrimaryKey 1..1 boolean "Primary key member"
* countryCode 1..1 BackboneElement ""
* countryCode.fk1 1..1 BackboneElement "Foreign key to Region"
* countryCode.fk1.reference 1..1 Reference(Region) ""
* countryCode.fk1.targetColumn 1..1 string ""
* regionCode 1..1 BackboneElement ""
* regionCode.fk1 1..1 BackboneElement "Foreign key to Region"
* regionCode.fk1.reference 1..1 Reference(Region) ""
* regionCode.fk1.targetColumn 1..1 string ""
* responsibleId 0..1 BackboneElement ""
* responsibleId.fk1 1..1 BackboneElement "Foreign key to Person"
* responsibleId.fk1.reference 1..1 Reference(Person) ""
* responsibleId.fk1.targetColumn 1..1 string ""
* responsibleId.fk2 1..1 BackboneElement "Foreign key to Organization"
* responsibleId.fk2.reference 1..1 Reference(Organization) ""
* responsibleId.fk2.targetColumn 1..1 string ""

