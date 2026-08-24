// ============================================================
// Généré automatiquement par Sashimi — ne pas modifier manuellement
// ============================================================
Logical: OsMupMessungen
Parent: Base
Id: os-mup-messungen
Title: "OS_MUP.MESSUNGEN"
Description: """Faux Comment
avec retour à la ligne"""
Characteristics: #can-be-target

* obeys sys-c00121080
* obeys sys-c00121081
* obeys sys-c00121082
* obeys sys-c00121083

* wertid 1..1 BackboneElement ""
* wertid.value 1..1 decimal "Primary Key"
* wertid.isPrimaryKey 1..1 boolean "Primary key member"
* wertid.precision 1..1 integer "Numeric precision"
* methodenid 0..1 BackboneElement ""
* methodenid.value 0..1 decimal "deprecated: \"use MEASURINGMETHOD\""
* methodenid.precision 1..1 integer "Numeric precision"
* punktortid 0..1 BackboneElement ""
* punktortid.value 0..1 decimal ""
* punktortid.precision 1..1 integer "Numeric precision"
* dropEinhid 0..1 BackboneElement ""
* dropEinhid.value 0..1 decimal ""
* dropEinhid.precision 1..1 integer "Numeric precision"
* zugId 0..1 BackboneElement ""
* zugId.precision 1..1 integer "Numeric precision"
* zugId.fk1 1..1 BackboneElement "Foreign key to OsMupZugaenge"
* zugId.fk1.reference 1..1 Reference(OsMupZugaenge) ""
* zugId.fk1.targetColumn 1..1 string ""
* zeitpunkt 1..1 date ""
* bem 0..1 string ""
* bem ^maxLength = 4000
* protid 0..1 BackboneElement ""
* protid.precision 1..1 integer "Numeric precision"
* protid.fk1 1..1 BackboneElement "Foreign key to OsKernProtokolle"
* protid.fk1.reference 1..1 Reference(OsKernProtokolle) ""
* protid.fk1.targetColumn 1..1 string ""
* dropInttagid 0..1 BackboneElement ""
* dropInttagid.value 0..1 decimal ""
* dropInttagid.precision 1..1 integer "Numeric precision"
* befundartid 1..1 BackboneElement ""
* befundartid.precision 1..1 integer "Numeric precision"
* befundartid.fk1 1..1 BackboneElement "Foreign key to OsMedBefundart"
* befundartid.fk1.reference 1..1 Reference(OsMedBefundart) ""
* befundartid.fk1.targetColumn 1..1 string ""
* einheitid 0..1 BackboneElement ""
* einheitid.value 0..1 decimal ""
* einheitid.precision 1..1 integer "Numeric precision"
* aufenthTherplanid 0..1 BackboneElement ""
* aufenthTherplanid.precision 1..1 integer "Numeric precision"
* aufenthTherplanid.fk1 1..1 BackboneElement "Foreign key to OsMedAufenthTherplan"
* aufenthTherplanid.fk1.reference 1..1 Reference(OsMedAufenthTherplan) ""
* aufenthTherplanid.fk1.targetColumn 1..1 string ""
* stornoDatum 0..1 date ""
* stornoMaid 0..1 BackboneElement ""
* stornoMaid.precision 1..1 integer "Numeric precision"
* stornoMaid.fk1 1..1 BackboneElement "Foreign key to OsKernMitarbeiter"
* stornoMaid.fk1.reference 1..1 Reference(OsKernMitarbeiter) ""
* stornoMaid.fk1.targetColumn 1..1 string ""
* komplikation 0..1 string ""
* komplikation ^maxLength = 254
* fallid 0..1 BackboneElement ""
* fallid.precision 1..1 integer "Numeric precision"
* fallid.fk1 1..1 BackboneElement "Foreign key to OsKernFall"
* fallid.fk1.reference 1..1 Reference(OsKernFall) ""
* fallid.fk1.targetColumn 1..1 string ""
* validDate 0..1 date ""
* validMaid 0..1 BackboneElement ""
* validMaid.precision 1..1 integer "Numeric precision"
* validMaid.fk1 1..1 BackboneElement "Foreign key to OsKernMitarbeiter"
* validMaid.fk1.reference 1..1 Reference(OsKernMitarbeiter) ""
* validMaid.fk1.targetColumn 1..1 string ""
* textvalue 0..1 string ""
* textvalue ^maxLength = 254
* megsjobId 0..1 BackboneElement ""
* megsjobId.precision 1..1 integer "Numeric precision"
* megsjobId.fk1 1..1 BackboneElement "Foreign key to OsMupMegsjob"
* megsjobId.fk1.reference 1..1 Reference(OsMupMegsjob) ""
* megsjobId.fk1.targetColumn 1..1 string ""
* measuringmethod 0..1 BackboneElement ""
* measuringmethod.precision 1..1 integer "Numeric precision"
* measuringmethod.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* measuringmethod.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* measuringmethod.fk1.targetColumn 1..1 string ""
* localisation 0..1 BackboneElement ""
* localisation.precision 1..1 integer "Numeric precision"
* localisation.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* localisation.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* localisation.fk1.targetColumn 1..1 string ""
* unit 0..1 BackboneElement ""
* unit.precision 1..1 integer "Numeric precision"
* unit.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* unit.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* unit.fk1.targetColumn 1..1 string ""
* megsparameterid 0..1 BackboneElement ""
* megsparameterid.value 0..1 decimal ""
* megsparameterid.precision 1..1 integer "Numeric precision"
* primitivum 0..1 BackboneElement ""
* primitivum.value 0..1 decimal ""
* primitivum.precision 1..1 integer "Numeric precision"
* relevance 0..1 BackboneElement ""
* relevance.precision 1..1 integer "Numeric precision"
* relevance.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* relevance.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* relevance.fk1.targetColumn 1..1 string ""
* origin 0..1 BackboneElement ""
* origin.precision 1..1 integer "Numeric precision"
* origin.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* origin.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* origin.fk1.targetColumn 1..1 string ""
* felddefinition 0..1 BackboneElement ""
* felddefinition.value 0..1 decimal ""
* felddefinition.precision 1..1 integer "Numeric precision"
* subitemnummer 0..1 BackboneElement ""
* subitemnummer.value 0..1 decimal ""
* subitemnummer.precision 1..1 integer "Numeric precision"
* laufnummer 0..1 BackboneElement ""
* laufnummer.value 0..1 decimal ""
* laufnummer.precision 1..1 integer "Numeric precision"
* measurementstatusid 1..1 BackboneElement ""
* measurementstatusid.precision 1..1 integer "Numeric precision"
* measurementstatusid.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* measurementstatusid.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* measurementstatusid.fk1.targetColumn 1..1 string ""
* wert 0..1 BackboneElement ""
* wert.value 0..1 decimal ""
* wert.precision 1..1 integer "Numeric precision"
* wert.scale 0..1 integer "Numeric scale"
* documentsubid 0..1 BackboneElement ""
* documentsubid.value 0..1 decimal ""
* documentsubid.precision 1..1 integer "Numeric precision"


Invariant: sys-c00121080
Description: "\"wertid\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00121081
Description: "\"zeitpunkt\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00121082
Description: "\"befundartid\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00121083
Description: "\"measurementstatusid\" IS NOT NULL"
Expression: "true"
Severity: #error

