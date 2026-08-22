// ============================================================
// Généré automatiquement par Sashimi — ne pas modifier manuellement
// ============================================================
Logical: OsKernFall
Parent: Base
Id: os-kern-fall
Title: "OS_KERN.FALL"
Characteristics: #can-be-target

* obeys b-dt-bit-1139320821
* obeys b-dt-bit-1391092559
* obeys b-dt-bit-139947068
* obeys b-dt-bit-1706657627
* obeys b-dt-bit-836431922
* obeys b-dt-date-1089066878
* obeys b-dt-date-1327138343
* obeys b-dt-date-1794704147
* obeys b-dt-date-256052664
* obeys b-dt-date-541975750
* obeys b-dt-date-876830750
* obeys ch10-fall
* obeys ch11-fall
* obeys ch12-fall
* obeys ch15-fall
* obeys ch17-fall
* obeys ch18-fall
* obeys ch1-fall
* obeys ch20-fall
* obeys ch21-fall
* obeys ch22-fall
* obeys ch3-fall
* obeys ch4-fall
* obeys ch5-fall
* obeys ch8-fall
* obeys ch9-fall
* obeys sys-c00108111
* obeys sys-c00108112
* obeys sys-c00108113
* obeys sys-c00108114
* obeys sys-c00108115
* obeys sys-c00108116
* obeys sys-c00108117
* obeys sys-c00108118
* obeys sys-c00108119

* fallid 1..1 decimal ""
* fallid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* fallid ^extension[=].valueString = "(38)"
* fallid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-is-pk"
* fallid ^extension[=].valueBoolean = true
* fallid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-unique"
* fallid ^extension[=].valueString = "UK2_FALL [UNIQUE]"
* entlfaehigid 0..1 string ""
* entlfaehigid ^maxLength = 2
* falltyp 0..1 decimal ""
* falltyp ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* falltyp ^extension[=].valueString = "(38)"
* arztid 0..1 Reference(OsKernPhysician) ""
* arztid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* arztid ^extension[=].valueString = "(38)"
* arztid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* arztid ^extension[=].extension[+].url = "targetColumn"
* arztid ^extension[=].extension[=].valueString = "physicianid"
* aufnahmegrundid 0..1 string ""
* aufnahmegrundid ^maxLength = 30
* arzArztid 0..1 Reference(OsKernPhysician) ""
* arzArztid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* arzArztid ^extension[=].valueString = "(38)"
* arzArztid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* arzArztid ^extension[=].extension[+].url = "targetColumn"
* arzArztid ^extension[=].extension[=].valueString = "physicianid"
* aufnahmeartid 0..1 string ""
* aufnahmeartid ^maxLength = 30
* entlassartid 0..1 string ""
* entlassartid ^maxLength = 30
* persnr 1..1 Reference(OsKernPatient) ""
* persnr ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* persnr ^extension[=].valueString = "(38)"
* persnr ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* persnr ^extension[=].extension[+].url = "targetColumn"
* persnr ^extension[=].extension[=].valueString = "persnr"
* fallnr 0..1 string ""
* fallnr ^maxLength = 20
* fallnr ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-unique"
* fallnr ^extension[=].valueString = "UK1_FALL [UNIQUE]"
* aktstatnr 0..1 decimal ""
* aktstatnr ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* aktstatnr ^extension[=].valueString = "(38)"
* anzeigen 0..1 string ""
* anzeigen ^maxLength = 1
* aufndat 0..1 date ""
* entldat 0..1 date ""
* vEntldat 0..1 date ""
* edate 0..1 date ""
* benid 0..1 string ""
* benid ^maxLength = 8
* mandantid 1..1 Reference(OsKernPmMandant) ""
* mandantid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* mandantid ^extension[=].valueString = "(38)"
* mandantid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* mandantid ^extension[=].extension[+].url = "targetColumn"
* mandantid ^extension[=].extension[=].valueString = "persnr"
* telefon 0..1 string ""
* telefon ^maxLength = 10
* vorstAdat 0..1 date ""
* einzugsid 0..1 Reference(OsKernEinzugsgebiet) ""
* einzugsid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* einzugsid ^extension[=].extension[+].url = "targetColumn"
* einzugsid ^extension[=].extension[=].valueString = "einzugsid"
* abrechstat 0..1 decimal ""
* abrechstat ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* abrechstat ^extension[=].valueString = "(38)"
* faktfrei 0..1 date ""
* geplEntldat 0..1 date ""
* stornoartid 0..1 decimal ""
* stornoartid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* stornoartid ^extension[=].valueString = "(38)"
* stornoUserid 0..1 Reference(OsSysSecUser) ""
* stornoUserid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* stornoUserid ^extension[=].valueString = "(38)"
* stornoUserid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* stornoUserid ^extension[=].extension[+].url = "targetColumn"
* stornoUserid ^extension[=].extension[=].valueString = "userId"
* stornoDatum 0..1 date ""
* stornoDatum ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-unique"
* stornoDatum ^extension[=].valueString = "UK2_FALL [UNIQUE]"
* kurzaufn 0..1 string ""
* kurzaufn ^maxLength = 1
* khPersnr 0..1 Reference(OsKernGesellschaft) ""
* khPersnr ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* khPersnr ^extension[=].valueString = "(38)"
* khPersnr ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* khPersnr ^extension[=].extension[+].url = "targetColumn"
* khPersnr ^extension[=].extension[=].valueString = "persnr"
* lkz 0..1 string ""
* lkz ^maxLength = 3
* stornogrund 0..1 string ""
* stornogrund ^maxLength = 100
* neugeborenes 1..1 string ""
* neugeborenes ^maxLength = 1
* upno 0..1 decimal ""
* einreise 0..1 string ""
* einreise ^maxLength = 1
* aufenthaltsart 0..1 decimal ""
* aufenthaltsart ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* aufenthaltsart ^extension[=].valueString = "(38)"
* notfallstelle 0..1 string ""
* notfallstelle ^maxLength = 30
* umgemeldet 0..1 date ""
* aufnahmegewicht 0..1 decimal ""
* beharztid 0..1 Reference(OsKernPhysician) ""
* beharztid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* beharztid ^extension[=].valueString = "(38)"
* beharztid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* beharztid ^extension[=].extension[+].url = "targetColumn"
* beharztid ^extension[=].extension[=].valueString = "physicianid"
* beharztmaid 0..1 Reference(OsKernMitarbeiter) ""
* beharztmaid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* beharztmaid ^extension[=].valueString = "(38)"
* beharztmaid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* beharztmaid ^extension[=].extension[+].url = "targetColumn"
* beharztmaid ^extension[=].extension[=].valueString = "maid"
* relativefallnr 0..1 string ""
* relativefallnr ^maxLength = 30
* relativefallnr ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-unique"
* relativefallnr ^extension[=].valueString = "UK2_FALL [UNIQUE]"
* ngebmitrg 0..1 decimal ""
* ngebmitrg ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* ngebmitrg ^extension[=].valueString = "(38)"
* statistik 1..1 decimal ""
* statistik ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* statistik ^extension[=].valueString = "(1)"
* fallartid 0..1 Reference(OsKernFallarten) ""
* fallartid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* fallartid ^extension[=].valueString = "(38)"
* fallartid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* fallartid ^extension[=].extension[+].url = "targetColumn"
* fallartid ^extension[=].extension[=].valueString = "fallartid"
* kennzeichen 0..1 decimal ""
* kennzeichen ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* kennzeichen ^extension[=].valueString = "(38)"
* abrechdatum 0..1 date ""
* bitkennz 0..1 decimal ""
* bitkennz ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* bitkennz ^extension[=].valueString = "(38)"
* geplTage 0..1 decimal ""
* geplTage ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* geplTage ^extension[=].valueString = "(38)"
* termin 0..1 string ""
* termin ^maxLength = 1
* wunschtermin 0..1 date ""
* monatsbuchung 0..1 date ""
* einreisedatum 0..1 date ""
* entlassungszustandid 0..1 Reference(OsSysCatalogdef) ""
* entlassungszustandid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* entlassungszustandid ^extension[=].valueString = "(38)"
* entlassungszustandid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* entlassungszustandid ^extension[=].extension[+].url = "targetColumn"
* entlassungszustandid ^extension[=].extension[=].valueString = "dbuid"
* arbeitsunfaehigBis 0..1 date ""
* acl 0..1 string ""
* acl ^maxLength = 4000
* ehemaligeikNr 0..1 string ""
* ehemaligeikNr ^maxLength = 10
* dischargestaynr 0..1 string ""
* dischargestaynr ^maxLength = 38
* admissioncause 0..1 decimal ""
* admissioncause ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* admissioncause ^extension[=].valueString = "(38)"
* dischargestate 0..1 decimal ""
* dischargestate ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* dischargestate ^extension[=].valueString = "(38)"
* medicalcasetype 1..1 Reference(OsSysCatalogdef) ""
* medicalcasetype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* medicalcasetype ^extension[=].valueString = "(38)"
* medicalcasetype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* medicalcasetype ^extension[=].extension[+].url = "targetColumn"
* medicalcasetype ^extension[=].extension[=].valueString = "dbuid"
* admissionreason 0..1 Reference(OsSysCatalogdef) ""
* admissionreason ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* admissionreason ^extension[=].valueString = "(38)"
* admissionreason ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* admissionreason ^extension[=].extension[+].url = "targetColumn"
* admissionreason ^extension[=].extension[=].valueString = "dbuid"
* admissiontype 0..1 Reference(OsSysCatalogdef) ""
* admissiontype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* admissiontype ^extension[=].valueString = "(38)"
* admissiontype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* admissiontype ^extension[=].extension[+].url = "targetColumn"
* admissiontype ^extension[=].extension[=].valueString = "dbuid"
* dischargetype 0..1 Reference(OsSysCatalogdef) ""
* dischargetype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* dischargetype ^extension[=].valueString = "(38)"
* dischargetype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* dischargetype ^extension[=].extension[+].url = "targetColumn"
* dischargetype ^extension[=].extension[=].valueString = "dbuid"
* medicalcasestate 0..1 Reference(OsSysCatalogdef) ""
* medicalcasestate ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* medicalcasestate ^extension[=].valueString = "(38)"
* medicalcasestate ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* medicalcasestate ^extension[=].extension[+].url = "targetColumn"
* medicalcasestate ^extension[=].extension[=].valueString = "dbuid"
* medicalcasestaytype 1..1 decimal ""
* medicalcasestaytype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* medicalcasestaytype ^extension[=].valueString = "(38)"
* casemark 1..1 decimal ""
* casemark ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* casemark ^extension[=].valueString = "(38)"
* country 0..1 Reference(OsSysCountry) ""
* country ^maxLength = 2
* country ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* country ^extension[=].extension[+].url = "targetColumn"
* country ^extension[=].extension[=].valueString = "cc"
* accidenttype 0..1 decimal ""
* accidenttype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* accidenttype ^extension[=].valueString = "(38)"
* origin 0..1 decimal ""
* origin ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* origin ^extension[=].valueString = "(38)"
* admissionstaynr 0..1 string ""
* admissionstaynr ^maxLength = 38
* destination 0..1 decimal ""
* destination ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* destination ^extension[=].valueString = "(38)"
* cancellationtype 0..1 decimal ""
* cancellationtype ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* cancellationtype ^extension[=].valueString = "(38)"
* refinsurance 0..1 Reference(OsKernKotraeg) ""
* refinsurance ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* refinsurance ^extension[=].valueString = "(38)"
* refinsurance ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* refinsurance ^extension[=].extension[+].url = "targetColumn"
* refinsurance ^extension[=].extension[=].valueString = "persnr"
* entrycode 0..1 decimal ""
* entrycode ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* entrycode ^extension[=].valueString = "(38)"
* dischargecode 0..1 decimal ""
* dischargecode ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* dischargecode ^extension[=].valueString = "(38)"
* ehicardid 0..1 Reference(OsKernEhicard) ""
* ehicardid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* ehicardid ^extension[=].valueString = "(38)"
* ehicardid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* ehicardid ^extension[=].extension[+].url = "targetColumn"
* ehicardid ^extension[=].extension[=].valueString = "ehicardid"
* accidentdate 0..1 date ""
* militarycode 0..1 string ""
* militarycode ^maxLength = 3
* issuedate 0..1 date ""
* isidentified 0..1 decimal ""
* isidentified ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* isidentified ^extension[=].valueString = "(1)"
* physiciannaid 0..1 Reference(OsKernPhysicianNashipaccreditation) ""
* physiciannaid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* physiciannaid ^extension[=].valueString = "(38)"
* physiciannaid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* physiciannaid ^extension[=].extension[+].url = "targetColumn"
* physiciannaid ^extension[=].extension[=].valueString = "physiciannaid"
* projectid 0..1 Reference(OsRwProject) ""
* projectid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* projectid ^extension[=].valueString = "(38)"
* projectid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* projectid ^extension[=].extension[+].url = "targetColumn"
* projectid ^extension[=].extension[=].valueString = "projectid"
* caseadmissionmode 1..1 Reference(OsSysCatalogdef) ""
* caseadmissionmode ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* caseadmissionmode ^extension[=].valueString = "(38)"
* caseadmissionmode ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* caseadmissionmode ^extension[=].extension[+].url = "targetColumn"
* caseadmissionmode ^extension[=].extension[=].valueString = "dbuid"
* satisfactionSurvey 0..1 decimal ""
* satisfactionSurvey ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* satisfactionSurvey ^extension[=].valueString = "(1)"
* sealed 0..1 decimal ""
* sealed ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* sealed ^extension[=].valueString = "(1)"
* suppressInformation 0..1 decimal ""
* suppressInformation ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* suppressInformation ^extension[=].valueString = "(1)"
* accountingmode 0..1 decimal ""
* accountingmode ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* accountingmode ^extension[=].valueString = "(38)"
* governmentinstitutereferalno 0..1 string ""
* governmentinstitutereferalno ^maxLength = 40
* billerIdentifier 0..1 string ""
* billerIdentifier ^maxLength = 35
* recommendedHospital 0..1 Reference(OsKernGesellschaft) ""
* recommendedHospital ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* recommendedHospital ^extension[=].valueString = "(38)"
* recommendedHospital ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* recommendedHospital ^extension[=].extension[+].url = "targetColumn"
* recommendedHospital ^extension[=].extension[=].valueString = "persnr"
* gestationTime 0..1 string ""
* gestationTime ^maxLength = 3
* ldate 0..1 date ""
* physicianTeamid 0..1 Reference(OsKernPhysicianTeam) ""
* physicianTeamid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* physicianTeamid ^extension[=].valueString = "(38)"
* physicianTeamid ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* physicianTeamid ^extension[=].extension[+].url = "targetColumn"
* physicianTeamid ^extension[=].extension[=].valueString = "physicianTeamid"
* site 0..1 Reference(OsKernOrgaebene) ""
* site ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* site ^extension[=].valueString = "(38)"
* site ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* site ^extension[=].extension[+].url = "targetColumn"
* site ^extension[=].extension[=].valueString = "oebeneid"
* codeword 0..1 string ""
* codeword ^maxLength = 255
* aftercarePhysician 0..1 Reference(OsKernPhysician) ""
* aftercarePhysician ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-precision"
* aftercarePhysician ^extension[=].valueString = "(38)"
* aftercarePhysician ^extension[+].url = "https://interop.aphp.fr/fhir/StructureDefinition/ext-sql-fk-columns"
* aftercarePhysician ^extension[=].extension[+].url = "targetColumn"
* aftercarePhysician ^extension[=].extension[=].valueString = "physicianid"


Invariant: b-dt-bit-1139320821
Description: "suppressInformation IN (0, 1)"
Expression: "true"
Severity: #error


Invariant: b-dt-bit-1391092559
Description: "statistik IN (0, 1)"
Expression: "true"
Severity: #error


Invariant: b-dt-bit-139947068
Description: "satisfactionSurvey IN (0, 1)"
Expression: "true"
Severity: #error


Invariant: b-dt-bit-1706657627
Description: "sealed IN (0, 1)"
Expression: "true"
Severity: #error


Invariant: b-dt-bit-836431922
Description: "isidentified IN (0, 1)"
Expression: "true"
Severity: #error


Invariant: b-dt-date-1089066878
Description: "date_trunc('day', umgemeldet) = umgemeldet"
Expression: "true"
Severity: #error


Invariant: b-dt-date-1327138343
Description: "date_trunc('day', monatsbuchung) = monatsbuchung"
Expression: "true"
Severity: #error


Invariant: b-dt-date-1794704147
Description: "date_trunc('day', arbeitsunfaehigBis) = arbeitsunfaehigBis"
Expression: "true"
Severity: #error


Invariant: b-dt-date-256052664
Description: "date_trunc('day', einreisedatum) = einreisedatum"
Expression: "true"
Severity: #error


Invariant: b-dt-date-541975750
Description: "date_trunc('day', accidentdate) = accidentdate"
Expression: "true"
Severity: #error


Invariant: b-dt-date-876830750
Description: "date_trunc('day', wunschtermin) = wunschtermin"
Expression: "true"
Severity: #error


Invariant: ch10-fall
Description: "vorstAdat < aufndat"
Expression: "true"
Severity: #error


Invariant: ch11-fall
Description: "((neugeborenes = 'J' AND aufndat <= entldat) OR (neugeborenes = 'N' AND aufndat < entldat))"
Expression: "true"
Severity: #error


Invariant: ch12-fall
Description: "einreise = 'J'"
Expression: "true"
Severity: #error


Invariant: ch15-fall
Description: "(beharztid IS NULL OR beharztmaid IS NULL)"
Expression: "true"
Severity: #error


Invariant: ch17-fall
Description: "(falltyp <> 0 OR (vorstAdat IS NULL AND aufndat IS NULL AND aktstatnr IS NULL))"
Expression: "true"
Severity: #error


Invariant: ch18-fall
Description: "termin IN ('B', 'U')"
Expression: "true"
Severity: #error


Invariant: ch1-fall
Description: "(aktstatnr <> 10 OR vorstAdat IS NOT NULL)"
Expression: "true"
Severity: #error


Invariant: ch20-fall
Description: "date_trunc('month', monatsbuchung) = monatsbuchung"
Expression: "true"
Severity: #error


Invariant: ch21-fall
Description: "NOT (einreise IS NULL AND einreisedatum IS NOT NULL)"
Expression: "true"
Severity: #error


Invariant: ch22-fall
Description: "(entlfaehigid IN ('2', '4') OR arbeitsunfaehigBis IS NULL)"
Expression: "true"
Severity: #error


Invariant: ch3-fall
Description: "(date_trunc('minute', aufndat) = aufndat AND date_trunc('minute', entldat) = entldat AND date_trunc('minute', vorstAdat) = vorstAdat)"
Expression: "true"
Severity: #error


Invariant: ch4-fall
Description: "(fallnr <> '0' AND (fallnr IS NOT NULL OR falltyp NOT IN (1, 2, 5)))"
Expression: "true"
Severity: #error


Invariant: ch5-fall
Description: "((falltyp = 3 AND stornoDatum IS NOT NULL AND stornoUserid IS NOT NULL AND cancellationtype IS NOT NULL) OR (falltyp <> 3 AND stornoDatum IS NULL AND stornoUserid IS NULL AND cancellationtype IS NULL))"
Expression: "true"
Severity: #error


Invariant: ch8-fall
Description: "((falltyp = 2 AND entldat IS NOT NULL AND aufndat IS NOT NULL) OR falltyp <> 2)"
Expression: "true"
Severity: #error


Invariant: ch9-fall
Description: "neugeborenes IN ('J', 'N')"
Expression: "true"
Severity: #error


Invariant: sys-c00108111
Description: "\"fallid\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00108112
Description: "\"persnr\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00108113
Description: "\"mandantid\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00108114
Description: "\"neugeborenes\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00108115
Description: "\"statistik\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00108116
Description: "\"medicalcasetype\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00108117
Description: "\"medicalcasestaytype\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00108118
Description: "\"casemark\" IS NOT NULL"
Expression: "true"
Severity: #error


Invariant: sys-c00108119
Description: "\"caseadmissionmode\" IS NOT NULL"
Expression: "true"
Severity: #error

