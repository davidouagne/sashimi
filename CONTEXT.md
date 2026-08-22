# Sashimi — Domain Glossary

Sashimi translates a **SQL Table** (a `CREATE TABLE` in a DDL file) into a FHIR **Logical Model**, expressed as a **StructureDefinition** with one **ElementDefinition** per SQL Column.

## SQL-side vocabulary

- **SQL Table**: one `CREATE TABLE` statement — a name, a set of SQL Columns, and its constraints (Primary Key, Foreign Keys, Unique Keys, Check Constraints). May carry a table-level comment (`COMMENT ON TABLE`).
- **SQL Column**: one column of a SQL Table — a name, a SQL type (with optional length/precision/scale), a nullability, and an optional column-level comment (`COMMENT ON COLUMN`).
- **Primary Key**: the set of SQL Column names that uniquely identify a row in a SQL Table.
- **Foreign Key**: a set of local SQL Column names referencing a set of SQL Column names on a target SQL Table.
- **Unique Key**: a named set of SQL Column names constrained to unique values.
- **Check Constraint**: a named boolean condition attached to a SQL Table. A Check Constraint of the form `column IS NOT NULL` also forces that column's cardinality (see Not-Null-Forcing Column below), on top of being rendered as an Invariant.
- **Not-Null-Forcing Column**: a SQL Column made non-nullable by a Check Constraint even though its own DDL nullability clause allows NULL. Distinct from the SQL Column's own nullability, which is a separate DDL fact.

## FHIR-side vocabulary

- **Logical Model**: the FHIR StructureDefinition produced for one SQL Table (`kind = logical`, `derivation = specialization`).
- **Invariant**: an `ElementDefinition.constraint` on the Logical Model's root element, rendered from a Check Constraint's condition text.

## Mapping rules (SQL → FHIR)

| SQL-side                              | FHIR-side                                            |
|----------------------------------------|-------------------------------------------------------|
| SQL Table                              | Logical Model (StructureDefinition)                    |
| SQL Column                             | ElementDefinition                                       |
| SQL Column NOT NULL, or Not-Null-Forcing Column | ElementDefinition.min = 1                     |
| Primary Key member                     | `ext-sql-is-pk` extension on the ElementDefinition       |
| Foreign Key                            | ElementDefinition.type = Reference(target Logical Model) + `ext-sql-fk-columns` extension |
| Unique Key member                      | `ext-sql-unique` extension on the ElementDefinition      |
| Check Constraint                       | Invariant on the Logical Model's root element            |

## Notes

- The SQL-side vocabulary (SQL Table, SQL Column, Primary/Foreign/Unique Key, Check Constraint, Not-Null-Forcing Column) is the seam between `SqlTableParser` and `StructureDefinitionMapper` (see [wayfinder map #1](https://github.com/davidouagne/sashimi/issues/1), ticket #3): `SqlTableParser.parse()` returns fully-resolved SQL Tables — comments, keys, and checks already attached — so `StructureDefinitionMapper` never depends on jOOQ's internal QOM types.
