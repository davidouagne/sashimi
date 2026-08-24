# Sashimi

Outil CLI Kotlin/Quarkus qui parse un fichier SQL DDL et génère des **Logical Models FSH** (FHIR Shorthand).

## Stack

| Couche       | Librairie                                   |
|--------------|---------------------------------------------|
| CLI / Runner | Quarkus (`QuarkusApplication` + Picocli)    |
| Parsing SQL  | jOOQ `DSLContext.parser()`                  |
| Modèle FHIR  | HAPI FHIR R4 `StructureDefinition`          |
| Sortie       | FSH (FHIR Shorthand) sérialisé manuellement |

## Architecture

```
src/main/kotlin/fr/aphp/sashimi/
├── SashimiMain.kt                    # Point d'entrée Quarkus + commandes Picocli (SashimiCommand, TranscribeCommand)
├── FhirExtensions.kt                 # URLs d'extension FHIR partagées (EXT_CHARACTERISTICS)
├── parser/
│   ├── SqlTable.kt                   # Modèle de domaine : SqlTable, SqlColumn, SqlForeignKey…
│   └── SqlTableParser.kt             # jOOQ DDL parser → List<SqlTable>
├── mapper/
│   ├── StructureDefinitionMapper.kt  # SqlTable → StructureDefinition HAPI
│   ├── InvariantText.kt              # Texte brut d'une condition CHECK → texte d'invariant FSH
│   └── NamingConventions.kt          # snake_case → PascalCase/camelCase/kebab-case
└── writer/
    └── FshWriter.kt                  # StructureDefinition → texte FSH
```

Le glossaire du domaine (SQL Table, SQL Column, Logical Model, Invariant…) est dans `CONTEXT.md`.

## Build & Run

```bash
# Build
./gradlew build

# Utilisation (transcribe est la sous-commande, -o est un DOSSIER de sortie
# qui doit déjà exister)
mkdir -p output
java -jar build/sashimi-0.1.0-SNAPSHOT-runner.jar transcribe \
  --input src/test/resources/fixtures/patient-record/input.sql \
  --output output
```

`--dialect` change la casse des identifiants non quotés dans la sortie
(ex. `POSTGRES` les replie en minuscules, `DEFAULT` en majuscules) — les
fixtures de `src/test/resources/fixtures/` sont toutes générées sans
`--dialect` (donc `DEFAULT`).

### Arguments (sous-commande `transcribe`)

| Argument        | Obligatoire | Défaut                            | Description                                |
|------------------|-------------|-----------------------------------|---------------------------------------------|
| `-i`, `--input`  | ✅           | —                                  | Fichier SQL DDL à transcrire (`CREATE TABLE`) |
| `-o`, `--output` | ❌           | dossier du fichier d'entrée       | **Dossier** de sortie pour les `.fsh` générés (un par table) |
| `--dialect`      | ❌           | `DEFAULT`                          | Dialecte jOOQ pour le parsing : `POSTGRES`, `MYSQL`, `DEFAULT` |

Un fichier `StructureDefinition-<id>.fsh` est écrit par table trouvée dans le DDL.

## Exemple de sortie FSH

D'après `src/test/resources/fixtures/patient-record/input.sql` :

```fsh
Logical: PatientRecord
Parent: Base
Id: patient-record
Title: "PATIENT_RECORD"
Characteristics: #can-be-target

* id 1..1 BackboneElement ""
* id.value 1..1 uuid ""
* id.isPrimaryKey 1..1 boolean "Primary key member"
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
```

## Tests

```bash
./gradlew test
```

`PipelineFixtureTest` fait tourner le pipeline complet sur chaque cas de
`src/test/resources/fixtures/<cas>/` (`input.sql` + `expected.fsh` de
référence) et compare le FSH produit par égalité stricte. Ces fixtures
sont aussi les exemples de référence du projet — voir
`src/test/resources/fixtures/patient-record/` pour un cas simple, ou
`os-kern-fall/` / `os-mup-messungen/` pour des schémas réels avec clés
étrangères et contraintes CHECK.

## Prochaines étapes

- [ ] Intégration SUSHI pour valider le FSH généré
- [ ] Export optionnel en JSON (`StructureDefinition` sérialisé par HAPI)
