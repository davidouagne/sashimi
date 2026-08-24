# Sashimi

Kotlin/Quarkus CLI tool that parses a SQL DDL file and generates **FSH Logical Models** (FHIR Shorthand).

## Stack

| Layer        | Library                                     |
|--------------|---------------------------------------------|
| CLI / Runner | Quarkus (`QuarkusApplication` + Picocli)    |
| SQL parsing  | jOOQ `DSLContext.parser()`                  |
| FHIR model   | HAPI FHIR R4 `StructureDefinition`          |
| Output       | FSH (FHIR Shorthand), serialized manually   |

## Architecture

```
src/main/kotlin/fr/aphp/sashimi/
├── SashimiMain.kt                    # Quarkus entry point + Picocli commands (SashimiCommand, TranscribeCommand)
├── FhirExtensions.kt                 # Shared FHIR extension URLs (EXT_CHARACTERISTICS)
├── parser/
│   ├── SqlTable.kt                   # Domain model: SqlTable, SqlColumn, SqlForeignKey…
│   └── SqlTableParser.kt             # jOOQ DDL parser → List<SqlTable>
├── mapper/
│   ├── StructureDefinitionMapper.kt  # SqlTable → HAPI StructureDefinition
│   ├── InvariantText.kt              # Raw CHECK condition text → FSH invariant text
│   └── NamingConventions.kt          # snake_case → PascalCase/camelCase/kebab-case
└── writer/
    └── FshWriter.kt                  # StructureDefinition → FSH text
```

The domain glossary (SQL Table, SQL Column, Logical Model, Invariant…) lives in `CONTEXT.md`.

## Build & Run

```bash
# Build
./gradlew build

# Usage (transcribe is the subcommand, -o is an OUTPUT FOLDER
# that must already exist)
mkdir -p output
java -jar build/sashimi-0.1.0-SNAPSHOT-runner.jar transcribe \
  --input src/test/resources/fixtures/patient-record/input.sql \
  --output output
```

`--dialect` changes the casing of unquoted identifiers in the output
(e.g. `POSTGRES` folds them to lowercase, `DEFAULT` to uppercase) — the
fixtures under `src/test/resources/fixtures/` are all generated without
`--dialect` (so `DEFAULT`).

### Arguments (`transcribe` subcommand)

| Argument        | Required | Default                           | Description                                |
|------------------|-------------|-----------------------------------|---------------------------------------------|
| `-i`, `--input`  | ✅           | —                                  | SQL DDL file to transcribe (`CREATE TABLE`) |
| `-o`, `--output` | ❌           | the input file's folder           | **Folder** where the generated `.fsh` files are written (one per table) |
| `--dialect`      | ❌           | `DEFAULT`                          | jOOQ dialect for parsing: `POSTGRES`, `MYSQL`, `DEFAULT` |

One `StructureDefinition-<id>.fsh` file is written per table found in the DDL.

## Example FSH output

From `src/test/resources/fixtures/patient-record/input.sql`:

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

`PipelineFixtureTest` runs the full pipeline on each case under
`src/test/resources/fixtures/<case>/` (`input.sql` + a reference
`expected.fsh`) and compares the produced FSH by strict equality. These
fixtures are also the project's reference examples — see
`src/test/resources/fixtures/patient-record/` for a simple case, or
`os-kern-fall/` / `os-mup-messungen/` for real-world schemas with foreign
keys and CHECK constraints.

## Next steps

- [ ] SUSHI integration to validate the generated FSH
- [ ] Optional JSON export (`StructureDefinition` serialized by HAPI)

## License

MIT — see [LICENSE.md](LICENSE.md).
