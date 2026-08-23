# Spec: extensions → BackboneElement for kind=logical StructureDefinitions

Consolidated deliverable of [wayfinder map #18](https://github.com/davidouagne/sashimi/issues/18). This document is the map's spec output: it is **not implemented** — `StructureDefinitionMapper.kt` still emits the extension-based form described below as "Before" on every branch except this one. A separate implementation effort consumes this spec, regenerates the six golden fixtures, and updates the live `CONTEXT.md` on `main` to match (this branch's `CONTEXT.md` shows the target state — see that file on this branch for the drafted glossary update).

## Decisions consolidated

- [#19](https://github.com/davidouagne/sashimi/issues/19): confirmed FSH/SUSHI syntax — `BackboneElement` must be declared explicitly, named sibling sub-groups (not slicing) are the only valid mechanism for multiple FKs per column on a `kind = logical` StructureDefinition.
- [#20](https://github.com/davidouagne/sashimi/issues/20): PK/UNIQUE/precision children, and the `.value` child every wrapped column needs for its own SQL value.
- [#21](https://github.com/davidouagne/sashimi/issues/21): FK sub-groups (`.fkN.reference` / `.fkN.targetColumn`), and the rule that an FK column has *no* `.value` child (the reference already carries the value).

## General mapping rule

A column becomes a `BackboneElement`-typed wrapper **if and only if** it carries at least one of: Primary Key membership, Foreign Key participation, Unique Key membership, numeric precision. A column with none of these stays an unwrapped primitive element, exactly as today.

When wrapped, the children present are the union of whichever facts apply — every fact is an independent sibling under the same wrapper, **except** `.value`, which is mutually exclusive with any Foreign Key fact:

| Fact | Child | Type | Present when |
|---|---|---|---|
| The column's own SQL value | `.value` | original FHIR primitive type | Always, **unless** the column has ≥1 Foreign Key (the `.fkN.reference`(s) already carry the value) |
| Primary Key member | `.isPrimaryKey` | `boolean` | Only if true |
| Unique Key member | `.uniqueKeyName` | `string` (resolved name, e.g. `uq-code`, no `[UNIQUE]` suffix) | Only if member of a UNIQUE constraint |
| Numeric precision | `.precision` | `integer` | Only if `precision > 0` |
| Numeric scale | `.scale` | `integer` | Only if `scale > 0` |
| Foreign Key *N* | `.fkN.reference` + `.fkN.targetColumn` | `Reference(Target)` + `string` | Once per distinct FK the column participates in, always indexed from `fk1` |

The wrapper's own cardinality is the column's original min/max; `.value` is always `1..1`; each `.fkN` group is always `1..1`. `^maxLength` and the column's own `short`/definition text (from `column.comment`) move to `.value` when present — they have no home on an FK-only column (no `.value`), so they are dropped there (see #21: already semantically questionable on a `Reference`-typed element today).

No composition case is special-cased beyond this table — a column with PK + UNIQUE + precision (see `fallid` below) gets all three fact children as siblings, exactly like a column with only one fact would get just that one.

## Per-fixture expected shape

### `anonymous-constraints` — full before/after (from #20's resolution)

Covers: PK-only (`id`), UNIQUE-only (`code`), precision+UNIQUE (`a`, `b`). See [#20's resolution comment](https://github.com/davidouagne/sashimi/issues/20#issuecomment-5385821452) for the complete before/after FSH.

### `facility` — full before/after (from #21's resolution)

Covers: PK-only (`id`), single-FK-per-column within a composite constraint (`countryCode`, `regionCode` → `region`), multi-FK-on-one-column (`responsibleId` → `person` or `organization`). See [#21's resolution comment](https://github.com/davidouagne/sashimi/issues/21#issuecomment-5385836469) for the complete before/after FSH.

### `encounter` / `patient-record` — full after

Both fixtures render a different table (`encounter`, `patient_record`) from the same shared `input.sql`; neither table declares any Foreign Key. Only each table's own PK column is wrapped; every other column is untouched.

`encounter`, after:
```
* id 1..1 BackboneElement ""
  * value 1..1 uuid ""
  * isPrimaryKey 1..1 boolean "Primary key member"
* patientId 1..1 uuid ""
* encounterDate 1..1 dateTime ""
* dischargeDate 0..1 dateTime ""
* status 1..1 string ""
* status ^maxLength = 50
* note 0..1 string ""
```

`patient-record`, after:
```
* id 1..1 BackboneElement ""
  * value 1..1 uuid ""
  * isPrimaryKey 1..1 boolean "Primary key member"
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

### `os-mup-messungen` — representative excerpt (30 columns, not fully transcribed)

New composition this fixture exercises that #20/#21 didn't have a concrete example for: **precision + Foreign Key on the same column** (e.g. `zugId`, `protid`, `stornoMaid` — all `NUMBER(38)` columns that are also FKs). Per the general rule above, this is not a new decision: `.precision` is a sibling like any other non-`.value` fact, independent of whether the column also has FKs. Only `.value` is FK-exclusive.

`wertid` — PK + precision (no FK, no UNIQUE):
```
Before:
* wertid 1..1 decimal "Primary Key"
* wertid ^extension[+].url = ".../ext-sql-precision"
* wertid ^extension[=].valueString = "(38)"
* wertid ^extension[+].url = ".../ext-sql-is-pk"
* wertid ^extension[=].valueBoolean = true

After:
* wertid 1..1 BackboneElement ""
  * value 1..1 decimal "Primary Key"
  * isPrimaryKey 1..1 boolean "Primary key member"
  * precision 1..1 integer "Numeric precision"
```

`zugId` — precision + FK (no PK, no UNIQUE):
```
Before:
* zugId 0..1 Reference(OsMupZugaenge) ""
* zugId ^extension[+].url = ".../ext-sql-precision"
* zugId ^extension[=].valueString = "(38)"
* zugId ^extension[+].url = ".../ext-sql-fk-columns"
* zugId ^extension[=].extension[+].url = "targetColumn"
* zugId ^extension[=].extension[=].valueString = "zugId"

After:
* zugId 0..1 BackboneElement ""
  * precision 1..1 integer "Numeric precision"
  * fk1 1..1 BackboneElement "Foreign key to OsMupZugaenge"
    * reference 1..1 Reference(OsMupZugaenge) ""
    * targetColumn 1..1 string ""
```
(No `.value` — the FK consumes that role, per #21.)

`zeitpunkt`, `bem`, `komplikation`, `textvalue` and the other plain columns with no PK/UNIQUE/FK/precision fact are **unchanged** — they stay unwrapped primitives, exactly as today.

The remaining ~25 columns in this fixture (`methodenid`, `punktortid`, `dropEinhid`, `protid`, `dropInttagid`, `befundartid`, `einheitid`, `aufenthTherplanid`, `stornoMaid`, `fallid`, `validMaid`, `megsjobId`, `measuringmethod`, `localisation`, `unit`, `megsparameterid`, `primitivum`, `relevance`, `origin`, `felddefinition`, `subitemnummer`, `laufnummer`, `measurementstatusid`, `wert`, `documentsubid`) are each either "precision-only" or "precision+FK" — both patterns fully covered by the two worked examples above. Not transcribed individually; mechanical application of the general rule.

### `os-kern-fall` — representative excerpt (85 columns, not fully transcribed)

New composition this fixture exercises: **PK + UNIQUE + precision, all three on one column** (`fallid`), and a plain single-FK column (`stornoUserid`) alongside UNIQUE-only sibling members of the same composite constraint (`relativefallnr`, `stornoDatum`).

`fallid` — PK + UNIQUE (member of composite `UK2_FALL`) + precision, all three:
```
Before:
* fallid 1..1 decimal ""
* fallid ^extension[+].url = ".../ext-sql-precision"
* fallid ^extension[=].valueString = "(38)"
* fallid ^extension[+].url = ".../ext-sql-is-pk"
* fallid ^extension[=].valueBoolean = true
* fallid ^extension[+].url = ".../ext-sql-unique"
* fallid ^extension[=].valueString = "UK2_FALL [UNIQUE]"

After:
* fallid 1..1 BackboneElement ""
  * value 1..1 decimal ""
  * isPrimaryKey 1..1 boolean "Primary key member"
  * uniqueKeyName 1..1 string "Unique key name"
  * precision 1..1 integer "Numeric precision"
```

`stornoUserid` — plain single FK + precision (same pattern as `os-mup-messungen`'s `zugId`):
```
Before:
* stornoUserid 0..1 Reference(OsSysSecUser) ""
* stornoUserid ^extension[+].url = ".../ext-sql-precision"
* stornoUserid ^extension[=].valueString = "(38)"
* stornoUserid ^extension[+].url = ".../ext-sql-fk-columns"
* stornoUserid ^extension[=].extension[+].url = "targetColumn"
* stornoUserid ^extension[=].extension[=].valueString = "userId"

After:
* stornoUserid 0..1 BackboneElement ""
  * precision 1..1 integer "Numeric precision"
  * fk1 1..1 BackboneElement "Foreign key to OsSysSecUser"
    * reference 1..1 Reference(OsSysSecUser) ""
    * targetColumn 1..1 string ""
```

`relativefallnr` — UNIQUE-only (member of the same composite `UK2_FALL` as `fallid` and `stornoDatum`, no cross-column link marker between them, matches #10's existing precedent):
```
Before:
* relativefallnr 0..1 string ""
* relativefallnr ^maxLength = 30
* relativefallnr ^extension[+].url = ".../ext-sql-unique"
* relativefallnr ^extension[=].valueString = "UK2_FALL [UNIQUE]"

After:
* relativefallnr 0..1 BackboneElement ""
  * value 0..1 string ""
  * value ^maxLength = 30
  * uniqueKeyName 1..1 string "Unique key name"
```

`stornoDatum` — UNIQUE-only, same composite key, different SQL type (`date`):
```
Before:
* stornoDatum 0..1 date ""
* stornoDatum ^extension[+].url = ".../ext-sql-unique"
* stornoDatum ^extension[=].valueString = "UK2_FALL [UNIQUE]"

After:
* stornoDatum 0..1 BackboneElement ""
  * value 0..1 date ""
  * uniqueKeyName 1..1 string "Unique key name"
```

The remaining ~80 columns of this fixture are each one of the patterns already fully worked out above (plain unwrapped column, precision-only, precision+FK, UNIQUE-only, PK+UNIQUE+precision) or the CHECK-constraint invariants (`* obeys ...` block, entirely untouched by this migration — CHECK constraints already use FHIR's native constraint mechanism, not extensions). Not transcribed individually.

## Out of scope reminder

`Characteristics: #can-be-target` (the `ext-characteristics` extension, StructureDefinition-level) is untouched by this migration — see map #18's "Out of scope" section. Every worked example above keeps it unchanged at the top of the `Logical:` block.
