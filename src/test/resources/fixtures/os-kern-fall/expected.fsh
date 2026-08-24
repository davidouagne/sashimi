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

* fallid 1..1 BackboneElement ""
* fallid.value 1..1 decimal ""
* fallid.isPrimaryKey 1..1 boolean "Primary key member"
* fallid.isPrimaryKey = true
* fallid.uniqueKeyName 1..1 string "Unique key name"
* fallid.uniqueKeyName = "UK2_FALL"
* fallid.precision 1..1 integer "Numeric precision"
* fallid.precision = 38
* entlfaehigid 0..1 string ""
* entlfaehigid ^maxLength = 2
* falltyp 0..1 BackboneElement ""
* falltyp.value 0..1 decimal ""
* falltyp.precision 1..1 integer "Numeric precision"
* falltyp.precision = 38
* arztid 0..1 BackboneElement ""
* arztid.precision 1..1 integer "Numeric precision"
* arztid.precision = 38
* arztid.fk1 1..1 BackboneElement "Foreign key to OsKernPhysician"
* arztid.fk1.reference 1..1 Reference(OsKernPhysician) ""
* arztid.fk1.targetColumn 1..1 string ""
* arztid.fk1.targetColumn = "physicianid"
* aufnahmegrundid 0..1 string ""
* aufnahmegrundid ^maxLength = 30
* arzArztid 0..1 BackboneElement ""
* arzArztid.precision 1..1 integer "Numeric precision"
* arzArztid.precision = 38
* arzArztid.fk1 1..1 BackboneElement "Foreign key to OsKernPhysician"
* arzArztid.fk1.reference 1..1 Reference(OsKernPhysician) ""
* arzArztid.fk1.targetColumn 1..1 string ""
* arzArztid.fk1.targetColumn = "physicianid"
* aufnahmeartid 0..1 string ""
* aufnahmeartid ^maxLength = 30
* entlassartid 0..1 string ""
* entlassartid ^maxLength = 30
* persnr 1..1 BackboneElement ""
* persnr.precision 1..1 integer "Numeric precision"
* persnr.precision = 38
* persnr.fk1 1..1 BackboneElement "Foreign key to OsKernPatient"
* persnr.fk1.reference 1..1 Reference(OsKernPatient) ""
* persnr.fk1.targetColumn 1..1 string ""
* persnr.fk1.targetColumn = "persnr"
* fallnr 0..1 BackboneElement ""
* fallnr.value 0..1 string ""
* fallnr.value ^maxLength = 20
* fallnr.uniqueKeyName 1..1 string "Unique key name"
* fallnr.uniqueKeyName = "UK1_FALL"
* aktstatnr 0..1 BackboneElement ""
* aktstatnr.value 0..1 decimal ""
* aktstatnr.precision 1..1 integer "Numeric precision"
* aktstatnr.precision = 38
* anzeigen 0..1 string ""
* anzeigen ^maxLength = 1
* aufndat 0..1 date ""
* entldat 0..1 date ""
* vEntldat 0..1 date ""
* edate 0..1 date ""
* benid 0..1 string ""
* benid ^maxLength = 8
* mandantid 1..1 BackboneElement ""
* mandantid.precision 1..1 integer "Numeric precision"
* mandantid.precision = 38
* mandantid.fk1 1..1 BackboneElement "Foreign key to OsKernPmMandant"
* mandantid.fk1.reference 1..1 Reference(OsKernPmMandant) ""
* mandantid.fk1.targetColumn 1..1 string ""
* mandantid.fk1.targetColumn = "persnr"
* telefon 0..1 string ""
* telefon ^maxLength = 10
* vorstAdat 0..1 date ""
* einzugsid 0..1 BackboneElement ""
* einzugsid.fk1 1..1 BackboneElement "Foreign key to OsKernEinzugsgebiet"
* einzugsid.fk1.reference 1..1 Reference(OsKernEinzugsgebiet) ""
* einzugsid.fk1.targetColumn 1..1 string ""
* einzugsid.fk1.targetColumn = "einzugsid"
* abrechstat 0..1 BackboneElement ""
* abrechstat.value 0..1 decimal ""
* abrechstat.precision 1..1 integer "Numeric precision"
* abrechstat.precision = 38
* faktfrei 0..1 date ""
* geplEntldat 0..1 date ""
* stornoartid 0..1 BackboneElement ""
* stornoartid.value 0..1 decimal ""
* stornoartid.precision 1..1 integer "Numeric precision"
* stornoartid.precision = 38
* stornoUserid 0..1 BackboneElement ""
* stornoUserid.precision 1..1 integer "Numeric precision"
* stornoUserid.precision = 38
* stornoUserid.fk1 1..1 BackboneElement "Foreign key to OsSysSecUser"
* stornoUserid.fk1.reference 1..1 Reference(OsSysSecUser) ""
* stornoUserid.fk1.targetColumn 1..1 string ""
* stornoUserid.fk1.targetColumn = "userId"
* stornoDatum 0..1 BackboneElement ""
* stornoDatum.value 0..1 date ""
* stornoDatum.uniqueKeyName 1..1 string "Unique key name"
* stornoDatum.uniqueKeyName = "UK2_FALL"
* kurzaufn 0..1 string ""
* kurzaufn ^maxLength = 1
* khPersnr 0..1 BackboneElement ""
* khPersnr.precision 1..1 integer "Numeric precision"
* khPersnr.precision = 38
* khPersnr.fk1 1..1 BackboneElement "Foreign key to OsKernGesellschaft"
* khPersnr.fk1.reference 1..1 Reference(OsKernGesellschaft) ""
* khPersnr.fk1.targetColumn 1..1 string ""
* khPersnr.fk1.targetColumn = "persnr"
* lkz 0..1 string ""
* lkz ^maxLength = 3
* stornogrund 0..1 string ""
* stornogrund ^maxLength = 100
* neugeborenes 1..1 string ""
* neugeborenes ^maxLength = 1
* upno 0..1 decimal ""
* einreise 0..1 string ""
* einreise ^maxLength = 1
* aufenthaltsart 0..1 BackboneElement ""
* aufenthaltsart.value 0..1 decimal ""
* aufenthaltsart.precision 1..1 integer "Numeric precision"
* aufenthaltsart.precision = 38
* notfallstelle 0..1 string ""
* notfallstelle ^maxLength = 30
* umgemeldet 0..1 date ""
* aufnahmegewicht 0..1 decimal ""
* beharztid 0..1 BackboneElement ""
* beharztid.precision 1..1 integer "Numeric precision"
* beharztid.precision = 38
* beharztid.fk1 1..1 BackboneElement "Foreign key to OsKernPhysician"
* beharztid.fk1.reference 1..1 Reference(OsKernPhysician) ""
* beharztid.fk1.targetColumn 1..1 string ""
* beharztid.fk1.targetColumn = "physicianid"
* beharztmaid 0..1 BackboneElement ""
* beharztmaid.precision 1..1 integer "Numeric precision"
* beharztmaid.precision = 38
* beharztmaid.fk1 1..1 BackboneElement "Foreign key to OsKernMitarbeiter"
* beharztmaid.fk1.reference 1..1 Reference(OsKernMitarbeiter) ""
* beharztmaid.fk1.targetColumn 1..1 string ""
* beharztmaid.fk1.targetColumn = "maid"
* relativefallnr 0..1 BackboneElement ""
* relativefallnr.value 0..1 string ""
* relativefallnr.value ^maxLength = 30
* relativefallnr.uniqueKeyName 1..1 string "Unique key name"
* relativefallnr.uniqueKeyName = "UK2_FALL"
* ngebmitrg 0..1 BackboneElement ""
* ngebmitrg.value 0..1 decimal ""
* ngebmitrg.precision 1..1 integer "Numeric precision"
* ngebmitrg.precision = 38
* statistik 1..1 BackboneElement ""
* statistik.value 1..1 decimal ""
* statistik.precision 1..1 integer "Numeric precision"
* statistik.precision = 1
* fallartid 0..1 BackboneElement ""
* fallartid.precision 1..1 integer "Numeric precision"
* fallartid.precision = 38
* fallartid.fk1 1..1 BackboneElement "Foreign key to OsKernFallarten"
* fallartid.fk1.reference 1..1 Reference(OsKernFallarten) ""
* fallartid.fk1.targetColumn 1..1 string ""
* fallartid.fk1.targetColumn = "fallartid"
* kennzeichen 0..1 BackboneElement ""
* kennzeichen.value 0..1 decimal ""
* kennzeichen.precision 1..1 integer "Numeric precision"
* kennzeichen.precision = 38
* abrechdatum 0..1 date ""
* bitkennz 0..1 BackboneElement ""
* bitkennz.value 0..1 decimal ""
* bitkennz.precision 1..1 integer "Numeric precision"
* bitkennz.precision = 38
* geplTage 0..1 BackboneElement ""
* geplTage.value 0..1 decimal ""
* geplTage.precision 1..1 integer "Numeric precision"
* geplTage.precision = 38
* termin 0..1 string ""
* termin ^maxLength = 1
* wunschtermin 0..1 date ""
* monatsbuchung 0..1 date ""
* einreisedatum 0..1 date ""
* entlassungszustandid 0..1 BackboneElement ""
* entlassungszustandid.precision 1..1 integer "Numeric precision"
* entlassungszustandid.precision = 38
* entlassungszustandid.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* entlassungszustandid.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* entlassungszustandid.fk1.targetColumn 1..1 string ""
* entlassungszustandid.fk1.targetColumn = "dbuid"
* arbeitsunfaehigBis 0..1 date ""
* acl 0..1 string ""
* acl ^maxLength = 4000
* ehemaligeikNr 0..1 string ""
* ehemaligeikNr ^maxLength = 10
* dischargestaynr 0..1 string ""
* dischargestaynr ^maxLength = 38
* admissioncause 0..1 BackboneElement ""
* admissioncause.value 0..1 decimal ""
* admissioncause.precision 1..1 integer "Numeric precision"
* admissioncause.precision = 38
* dischargestate 0..1 BackboneElement ""
* dischargestate.value 0..1 decimal ""
* dischargestate.precision 1..1 integer "Numeric precision"
* dischargestate.precision = 38
* medicalcasetype 1..1 BackboneElement ""
* medicalcasetype.precision 1..1 integer "Numeric precision"
* medicalcasetype.precision = 38
* medicalcasetype.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* medicalcasetype.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* medicalcasetype.fk1.targetColumn 1..1 string ""
* medicalcasetype.fk1.targetColumn = "dbuid"
* admissionreason 0..1 BackboneElement ""
* admissionreason.precision 1..1 integer "Numeric precision"
* admissionreason.precision = 38
* admissionreason.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* admissionreason.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* admissionreason.fk1.targetColumn 1..1 string ""
* admissionreason.fk1.targetColumn = "dbuid"
* admissiontype 0..1 BackboneElement ""
* admissiontype.precision 1..1 integer "Numeric precision"
* admissiontype.precision = 38
* admissiontype.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* admissiontype.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* admissiontype.fk1.targetColumn 1..1 string ""
* admissiontype.fk1.targetColumn = "dbuid"
* dischargetype 0..1 BackboneElement ""
* dischargetype.precision 1..1 integer "Numeric precision"
* dischargetype.precision = 38
* dischargetype.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* dischargetype.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* dischargetype.fk1.targetColumn 1..1 string ""
* dischargetype.fk1.targetColumn = "dbuid"
* medicalcasestate 0..1 BackboneElement ""
* medicalcasestate.precision 1..1 integer "Numeric precision"
* medicalcasestate.precision = 38
* medicalcasestate.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* medicalcasestate.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* medicalcasestate.fk1.targetColumn 1..1 string ""
* medicalcasestate.fk1.targetColumn = "dbuid"
* medicalcasestaytype 1..1 BackboneElement ""
* medicalcasestaytype.value 1..1 decimal ""
* medicalcasestaytype.precision 1..1 integer "Numeric precision"
* medicalcasestaytype.precision = 38
* casemark 1..1 BackboneElement ""
* casemark.value 1..1 decimal ""
* casemark.precision 1..1 integer "Numeric precision"
* casemark.precision = 38
* country 0..1 BackboneElement ""
* country.fk1 1..1 BackboneElement "Foreign key to OsSysCountry"
* country.fk1.reference 1..1 Reference(OsSysCountry) ""
* country.fk1.targetColumn 1..1 string ""
* country.fk1.targetColumn = "cc"
* accidenttype 0..1 BackboneElement ""
* accidenttype.value 0..1 decimal ""
* accidenttype.precision 1..1 integer "Numeric precision"
* accidenttype.precision = 38
* origin 0..1 BackboneElement ""
* origin.value 0..1 decimal ""
* origin.precision 1..1 integer "Numeric precision"
* origin.precision = 38
* admissionstaynr 0..1 string ""
* admissionstaynr ^maxLength = 38
* destination 0..1 BackboneElement ""
* destination.value 0..1 decimal ""
* destination.precision 1..1 integer "Numeric precision"
* destination.precision = 38
* cancellationtype 0..1 BackboneElement ""
* cancellationtype.value 0..1 decimal ""
* cancellationtype.precision 1..1 integer "Numeric precision"
* cancellationtype.precision = 38
* refinsurance 0..1 BackboneElement ""
* refinsurance.precision 1..1 integer "Numeric precision"
* refinsurance.precision = 38
* refinsurance.fk1 1..1 BackboneElement "Foreign key to OsKernKotraeg"
* refinsurance.fk1.reference 1..1 Reference(OsKernKotraeg) ""
* refinsurance.fk1.targetColumn 1..1 string ""
* refinsurance.fk1.targetColumn = "persnr"
* entrycode 0..1 BackboneElement ""
* entrycode.value 0..1 decimal ""
* entrycode.precision 1..1 integer "Numeric precision"
* entrycode.precision = 38
* dischargecode 0..1 BackboneElement ""
* dischargecode.value 0..1 decimal ""
* dischargecode.precision 1..1 integer "Numeric precision"
* dischargecode.precision = 38
* ehicardid 0..1 BackboneElement ""
* ehicardid.precision 1..1 integer "Numeric precision"
* ehicardid.precision = 38
* ehicardid.fk1 1..1 BackboneElement "Foreign key to OsKernEhicard"
* ehicardid.fk1.reference 1..1 Reference(OsKernEhicard) ""
* ehicardid.fk1.targetColumn 1..1 string ""
* ehicardid.fk1.targetColumn = "ehicardid"
* accidentdate 0..1 date ""
* militarycode 0..1 string ""
* militarycode ^maxLength = 3
* issuedate 0..1 date ""
* isidentified 0..1 BackboneElement ""
* isidentified.value 0..1 decimal ""
* isidentified.precision 1..1 integer "Numeric precision"
* isidentified.precision = 1
* physiciannaid 0..1 BackboneElement ""
* physiciannaid.precision 1..1 integer "Numeric precision"
* physiciannaid.precision = 38
* physiciannaid.fk1 1..1 BackboneElement "Foreign key to OsKernPhysicianNashipaccreditation"
* physiciannaid.fk1.reference 1..1 Reference(OsKernPhysicianNashipaccreditation) ""
* physiciannaid.fk1.targetColumn 1..1 string ""
* physiciannaid.fk1.targetColumn = "physiciannaid"
* projectid 0..1 BackboneElement ""
* projectid.precision 1..1 integer "Numeric precision"
* projectid.precision = 38
* projectid.fk1 1..1 BackboneElement "Foreign key to OsRwProject"
* projectid.fk1.reference 1..1 Reference(OsRwProject) ""
* projectid.fk1.targetColumn 1..1 string ""
* projectid.fk1.targetColumn = "projectid"
* caseadmissionmode 1..1 BackboneElement ""
* caseadmissionmode.precision 1..1 integer "Numeric precision"
* caseadmissionmode.precision = 38
* caseadmissionmode.fk1 1..1 BackboneElement "Foreign key to OsSysCatalogdef"
* caseadmissionmode.fk1.reference 1..1 Reference(OsSysCatalogdef) ""
* caseadmissionmode.fk1.targetColumn 1..1 string ""
* caseadmissionmode.fk1.targetColumn = "dbuid"
* satisfactionSurvey 0..1 BackboneElement ""
* satisfactionSurvey.value 0..1 decimal ""
* satisfactionSurvey.precision 1..1 integer "Numeric precision"
* satisfactionSurvey.precision = 1
* sealed 0..1 BackboneElement ""
* sealed.value 0..1 decimal ""
* sealed.precision 1..1 integer "Numeric precision"
* sealed.precision = 1
* suppressInformation 0..1 BackboneElement ""
* suppressInformation.value 0..1 decimal ""
* suppressInformation.precision 1..1 integer "Numeric precision"
* suppressInformation.precision = 1
* accountingmode 0..1 BackboneElement ""
* accountingmode.value 0..1 decimal ""
* accountingmode.precision 1..1 integer "Numeric precision"
* accountingmode.precision = 38
* governmentinstitutereferalno 0..1 string ""
* governmentinstitutereferalno ^maxLength = 40
* billerIdentifier 0..1 string ""
* billerIdentifier ^maxLength = 35
* recommendedHospital 0..1 BackboneElement ""
* recommendedHospital.precision 1..1 integer "Numeric precision"
* recommendedHospital.precision = 38
* recommendedHospital.fk1 1..1 BackboneElement "Foreign key to OsKernGesellschaft"
* recommendedHospital.fk1.reference 1..1 Reference(OsKernGesellschaft) ""
* recommendedHospital.fk1.targetColumn 1..1 string ""
* recommendedHospital.fk1.targetColumn = "persnr"
* gestationTime 0..1 string ""
* gestationTime ^maxLength = 3
* ldate 0..1 date ""
* physicianTeamid 0..1 BackboneElement ""
* physicianTeamid.precision 1..1 integer "Numeric precision"
* physicianTeamid.precision = 38
* physicianTeamid.fk1 1..1 BackboneElement "Foreign key to OsKernPhysicianTeam"
* physicianTeamid.fk1.reference 1..1 Reference(OsKernPhysicianTeam) ""
* physicianTeamid.fk1.targetColumn 1..1 string ""
* physicianTeamid.fk1.targetColumn = "physicianTeamid"
* site 0..1 BackboneElement ""
* site.precision 1..1 integer "Numeric precision"
* site.precision = 38
* site.fk1 1..1 BackboneElement "Foreign key to OsKernOrgaebene"
* site.fk1.reference 1..1 Reference(OsKernOrgaebene) ""
* site.fk1.targetColumn 1..1 string ""
* site.fk1.targetColumn = "oebeneid"
* codeword 0..1 string ""
* codeword ^maxLength = 255
* aftercarePhysician 0..1 BackboneElement ""
* aftercarePhysician.precision 1..1 integer "Numeric precision"
* aftercarePhysician.precision = 38
* aftercarePhysician.fk1 1..1 BackboneElement "Foreign key to OsKernPhysician"
* aftercarePhysician.fk1.reference 1..1 Reference(OsKernPhysician) ""
* aftercarePhysician.fk1.targetColumn 1..1 string ""
* aftercarePhysician.fk1.targetColumn = "physicianid"


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

