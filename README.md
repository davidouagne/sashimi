# Sashimi

Outil CLI Kotlin/Spring Boot qui parse un fichier SQL DDL et génère des **Logical Models FSH** (FHIR Shorthand).

## Stack

| Couche       | Librairie                                   |
|--------------|---------------------------------------------|
| CLI / Runner | Spring Boot `CommandLineRunner`             |
| Parsing SQL  | jOOQ `DSLContext.parser()`                  |
| Modèle FHIR  | HAPI FHIR R4 `StructureDefinition`          |
| Sortie       | FSH (FHIR Shorthand) sérialisé manuellement |

## Architecture

```
src/main/kotlin/fr/aphp/sashimi/
├── SashimiApplication.kt             # Point d'entrée Spring Boot
├── cli/
│   └── Sql2FshRunner.kt              # CommandLineRunner – parsing des args
├── parser/
│   └── SqlTableParser.kt             # jOOQ DDL parser → List<SqlTable>
├── mapper/
│   └── StructureDefinitionMapper.kt  # SqlTable → StructureDefinition HAPI
└── writer/
    └── FshWriter.kt                  # StructureDefinition → texte FSH
```

## Build & Run

```bash
# Build
./gradlew bootJar

# Utilisation
java -jar build/libs/sashimi.jar \
  --input=sample.sql \
  --output=output.fsh \
  --dialect=POSTGRES
```

### Arguments

| Argument    | Obligatoire | Défaut        | Description                                |
|-------------|-------------|---------------|--------------------------------------------|
| `--input`   | ✅           | —             | Chemin vers le fichier SQL DDL             |
| `--output`  | ❌           | `<input>.fsh` | Fichier FSH de sortie                      |
| `--dialect` | ❌           | `DEFAULT`     | Dialecte jOOQ : `POSTGRES`, `MYSQL`, `H2`… |

## Exemple de sortie FSH

```fsh
Logical: PatientRecord
Id: patientrecord
Title: "Patient Record"

* id 1..1 uuid "Clé primaire SQL"
* ipp 1..1 string "IPP du patient"
* ipp ^maxLength = 20
* lastName 1..1 string "Nom du patient"
* firstName 1..1 string "Prénom du patient"
* birthDate 0..1 date "date de naissance"
* gender 0..1 string "Genre"
* active 1..1 boolean "enregistrement actif"
* createdAt 1..1 dateTime "date de création de l'enregistrement"
* updatedAt 0..1 dateTime "date de mise à jour de l'enregistrement"
```

## Tests

```bash
./gradlew test
```

## Prochaines étapes

- [ ] Intégration SUSHI pour valider le FSH généré
- [ ] Export optionnel en JSON (`StructureDefinition` sérialisé par HAPI)
