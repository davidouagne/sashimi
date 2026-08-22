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

* wertid 1..1 decimal "Primary Key"
* wertid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* wertid ^extension[=].valueString = "(38)"
* wertid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-is-pk"
* wertid ^extension[=].valueBoolean = true
* methodenid 0..1 decimal "deprecated: \"use MEASURINGMETHOD\""
* methodenid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* methodenid ^extension[=].valueString = "(38)"
* punktortid 0..1 decimal ""
* punktortid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* punktortid ^extension[=].valueString = "(38)"
* dropEinhid 0..1 decimal ""
* dropEinhid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* dropEinhid ^extension[=].valueString = "(38)"
* zugId 0..1 Reference(OsMupZugaenge) ""
* zugId ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* zugId ^extension[=].valueString = "(38)"
* zugId ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* zugId ^extension[=].extension[+].url = "targetColumn"
* zugId ^extension[=].extension[=].valueString = "zugId"
* zeitpunkt 1..1 date ""
* bem 0..1 string ""
* bem ^maxLength = 4000
* protid 0..1 Reference(OsKernProtokolle) ""
* protid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* protid ^extension[=].valueString = "(38)"
* protid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* protid ^extension[=].extension[+].url = "targetColumn"
* protid ^extension[=].extension[=].valueString = "protid"
* dropInttagid 0..1 decimal ""
* dropInttagid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* dropInttagid ^extension[=].valueString = "(38)"
* befundartid 1..1 Reference(OsMedBefundart) ""
* befundartid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* befundartid ^extension[=].valueString = "(38)"
* befundartid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* befundartid ^extension[=].extension[+].url = "targetColumn"
* befundartid ^extension[=].extension[=].valueString = "befundartid"
* einheitid 0..1 decimal ""
* einheitid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* einheitid ^extension[=].valueString = "(38)"
* aufenthTherplanid 0..1 Reference(OsMedAufenthTherplan) ""
* aufenthTherplanid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* aufenthTherplanid ^extension[=].valueString = "(38)"
* aufenthTherplanid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* aufenthTherplanid ^extension[=].extension[+].url = "targetColumn"
* aufenthTherplanid ^extension[=].extension[=].valueString = "aufenthTherplanid"
* stornoDatum 0..1 date ""
* stornoMaid 0..1 Reference(OsKernMitarbeiter) ""
* stornoMaid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* stornoMaid ^extension[=].valueString = "(38)"
* stornoMaid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* stornoMaid ^extension[=].extension[+].url = "targetColumn"
* stornoMaid ^extension[=].extension[=].valueString = "maid"
* komplikation 0..1 string ""
* komplikation ^maxLength = 254
* fallid 0..1 Reference(OsKernFall) ""
* fallid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* fallid ^extension[=].valueString = "(38)"
* fallid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* fallid ^extension[=].extension[+].url = "targetColumn"
* fallid ^extension[=].extension[=].valueString = "fallid"
* validDate 0..1 date ""
* validMaid 0..1 Reference(OsKernMitarbeiter) ""
* validMaid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* validMaid ^extension[=].valueString = "(38)"
* validMaid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* validMaid ^extension[=].extension[+].url = "targetColumn"
* validMaid ^extension[=].extension[=].valueString = "maid"
* textvalue 0..1 string ""
* textvalue ^maxLength = 254
* megsjobId 0..1 Reference(OsMupMegsjob) ""
* megsjobId ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* megsjobId ^extension[=].valueString = "(38)"
* megsjobId ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* megsjobId ^extension[=].extension[+].url = "targetColumn"
* megsjobId ^extension[=].extension[=].valueString = "megsjobId"
* measuringmethod 0..1 Reference(OsSysCatalogdef) ""
* measuringmethod ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* measuringmethod ^extension[=].valueString = "(38)"
* measuringmethod ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* measuringmethod ^extension[=].extension[+].url = "targetColumn"
* measuringmethod ^extension[=].extension[=].valueString = "dbuid"
* localisation 0..1 Reference(OsSysCatalogdef) ""
* localisation ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* localisation ^extension[=].valueString = "(38)"
* localisation ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* localisation ^extension[=].extension[+].url = "targetColumn"
* localisation ^extension[=].extension[=].valueString = "dbuid"
* unit 0..1 Reference(OsSysCatalogdef) ""
* unit ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* unit ^extension[=].valueString = "(38)"
* unit ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* unit ^extension[=].extension[+].url = "targetColumn"
* unit ^extension[=].extension[=].valueString = "dbuid"
* megsparameterid 0..1 decimal ""
* megsparameterid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* megsparameterid ^extension[=].valueString = "(38)"
* primitivum 0..1 decimal ""
* primitivum ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* primitivum ^extension[=].valueString = "(38)"
* relevance 0..1 Reference(OsSysCatalogdef) ""
* relevance ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* relevance ^extension[=].valueString = "(38)"
* relevance ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* relevance ^extension[=].extension[+].url = "targetColumn"
* relevance ^extension[=].extension[=].valueString = "dbuid"
* origin 0..1 Reference(OsSysCatalogdef) ""
* origin ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* origin ^extension[=].valueString = "(38)"
* origin ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* origin ^extension[=].extension[+].url = "targetColumn"
* origin ^extension[=].extension[=].valueString = "dbuid"
* felddefinition 0..1 decimal ""
* felddefinition ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* felddefinition ^extension[=].valueString = "(38)"
* subitemnummer 0..1 decimal ""
* subitemnummer ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* subitemnummer ^extension[=].valueString = "(38)"
* laufnummer 0..1 decimal ""
* laufnummer ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* laufnummer ^extension[=].valueString = "(38)"
* measurementstatusid 1..1 Reference(OsSysCatalogdef) ""
* measurementstatusid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* measurementstatusid ^extension[=].valueString = "(38)"
* measurementstatusid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* measurementstatusid ^extension[=].extension[+].url = "targetColumn"
* measurementstatusid ^extension[=].extension[=].valueString = "dbuid"
* wert 0..1 decimal ""
* wert ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* wert ^extension[=].valueString = "(12,4)"
* documentsubid 0..1 decimal ""
* documentsubid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* documentsubid ^extension[=].valueString = "(38)"


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

