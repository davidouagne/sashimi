# Research: FSH BackboneElement child syntax for SQL-column facts

**Date:** 2026-08-23
**Related:** [issue #19](https://github.com/davidouagne/sashimi/issues/19), [wayfinder map #18](https://github.com/davidouagne/sashimi/issues/18)

**What this confirms:** the repo has already decided to stop expressing SQL-column facts (Primary Key membership, Foreign Key target column, Unique Key membership, numeric precision/scale) as FHIR *extensions* on `ElementDefinition`s, and instead express them as nested `BackboneElement` children in the generated FSH (e.g. `Patient.id.isPrimaryKey: boolean`, `Patient.patientId.fk1.reference: Reference(Target)` + `Patient.patientId.fk1.targetColumn: string`). This repo has **no SUSHI compilation step** — its tests only diff generated FSH text against golden fixtures — so an invalid syntax choice here would go completely undetected until someone ran real SUSHI. This document verifies, against primary sources only, that the proposed syntax is valid FSH as understood by SUSHI's own Language Reference.

All citations below are to the **published FHIR Shorthand (FSH) Language Reference**, sourced from the canonical markdown that HL7 publishes to both `hl7.org/fhir/uv/shorthand/reference.html` (release) and `build.fhir.org/ig/HL7/fhir-shorthand/reference.html` (continuous build) — both render from `input/pagecontent/reference.md` in the [HL7/fhir-shorthand](https://github.com/HL7/fhir-shorthand) repo. Line numbers reference that markdown source as fetched 2026-08-23. Where useful, R4/R5 FHIR core spec pages are cited directly. fshschool.org's SUSHI docs were checked but contain no dedicated Logical Model authoring guide beyond what the Language Reference already covers — see Q1 below.

---

## Q1 — Implicit vs. explicit BackboneElement typing

**Confirmed syntax (explicit typing required):**

```
* col 0..1 BackboneElement "Column" "A SQL column"
* col.isPrimaryKey 1..1 boolean "Primary key member"
```

**Conclusion:** SUSHI does **not** implicitly infer `BackboneElement` from a bare parent path that only has children. The **Add Element Rule** — the rule type used to add new elements to Logical Models and Resources — explicitly requires a type on every rule, including the parent:

> "An add element rule **at minimum** MUST specify: an element path, cardinality, type, and short description, OR an element path, cardinality, the `contentReference` keyword, a content reference URI, and short description."
> — [FSH Language Reference, "Add Element Rules"](https://hl7.org/fhir/uv/shorthand/reference.html#add-element-rules)

The spec's own worked example for this exact pattern (a BackboneElement parent with three typed children) declares the parent's type explicitly as `BackboneElement`, then adds children by dotted path with no type on the parent's line ever being re-inferred — it was already stated once, up front:

```
* serviceAnimal 0..* BackboneElement "Service animals" "Animals trained to assist the person by performing certain tasks."
* serviceAnimal.name 0..1 string "Name of service animal" "The name by which the service animal responds."
* serviceAnimal.breed 1..* CodeableConcept "Breed of service animal" "The dominant breed or breeds of the service animal."
* serviceAnimal.startDate 0..1 date "Date the service animal began work" "The date on which the service animal began working for the person."
```
— [FSH Language Reference, "Add Element Rules" examples](https://hl7.org/fhir/uv/shorthand/reference.html#add-element-rules)

The idiomatic/preferred style shown immediately after in the same section uses indentation to avoid repeating the parent path prefix on each child line — this is the style the repo's generator should target:

```
* serviceAnimal 0..* BackboneElement "Service animals" "Animals trained to assist the person by performing certain tasks."
  * name 0..1 string "Name of service animal" "The name by which the service animal responds."
  * breed 1..* CodeableConcept "Breed of service animal" "The dominant breed or breeds of the service animal."
  * startDate 0..1 date "Date the service animal began work" "The date on which the service animal began working for the person."
```
— same citation as above.

Note: this is distinct from the *FHIR core spec's* general statement that "the descendant types of BackboneElement are all declared implicitly as part of the definitions of the resources" ([R4 BackboneElement](https://hl7.org/fhir/R4/backboneelement.html)) — that statement describes how the **compiled StructureDefinition** ends up with no separate reusable named type for the backbone's children (they're inline, resource-specific ElementDefinitions), not a claim that the **FSH source text** can omit the `BackboneElement` type keyword. At the FSH-authoring layer, the type keyword on the parent's Add Element rule is mandatory per the Language Reference quote above.

---

## Q2 — Typed leaf children under a BackboneElement path

**Confirmed syntax:**

```
* col 0..1 BackboneElement "Column" "A SQL column"
* col.isPrimaryKey 1..1 boolean "Primary key member"
* col.precision 1..1 integer "Numeric precision"
* col.scale 0..1 integer "Numeric scale"
```

**Conclusion:** Confirmed valid. This is exactly the Add Element Rule pattern from Q1 (`* <element> {min}..{max} {flag(s)} {datatype(s)} "{short}" "{definition}"`), applied per-child with dotted paths. The spec's own example (`serviceAnimal.name`, `serviceAnimal.breed`, `serviceAnimal.startDate`) is structurally identical — a BackboneElement parent with `string`, `CodeableConcept`, and `date` typed leaf children, added one Add Element rule per child, each ending with a dotted path off the parent.
— [FSH Language Reference, "Add Element Rules"](https://hl7.org/fhir/uv/shorthand/reference.html#add-element-rules)

Add Element Rules are confirmed applicable to `Logical` items specifically (not just `Resource`) in the rule-applicability table:

> "Rule types that apply to Logicals are: [Add Element](#add-element-rules), Assignment, Binding, Cardinality, Flag, Insert, Obeys, Path, and Type."
> — [FSH Language Reference, "Defining Logical Models"](https://hl7.org/fhir/uv/shorthand/reference.html#defining-logical-models), corroborated by Table 7 ("Relationships between FSH items and FSH rules"), same page, `#t7`, row "Add Element" → column "Logical" = `Y`.

---

## Q3 — Nested `Reference()` typing under a BackboneElement path

**Confirmed syntax:**

```
* col.fk1.reference 1..1 Reference(TargetLogicalModel) "Target row"
```

**Conclusion:** Confirmed valid and syntactically identical to a top-level Reference element. `Reference(...)` is just one of the allowed `{datatype(s)}` values in the Add Element Rule grammar, and the rule works the same regardless of how deep the element's path is:

> "`{datatype(s)}` MAY be one of the following: ... References to one or more resources or profiles, `Reference({Resource/Profile1} or {Resource/Profile2} or {Resource/Profile3}...)`"
> — [FSH Language Reference, "Add Element Rules"](https://hl7.org/fhir/uv/shorthand/reference.html#add-element-rules)

**Reference to a Logical Model — no special syntax, but one semantic prerequisite.** The Language Reference's own canonical Logical Model example references *another Logical Model* by name using the exact same `Reference(TypeName)` syntax used for resources — no special-casing:

```
Logical:        FamilyMember
...
* human 1..1 SU Reference(Human) "Family member" "A reference to the human family member"
```
— [FSH Language Reference, "Defining Logical Models"](https://hl7.org/fhir/uv/shorthand/reference.html#defining-logical-models) (full example quoted in the "Confirmed pattern" section below)

The one semantic (not syntactic) consideration: per FHIR's own StructureDefinition rules, a type can only be a valid `Reference` target if it is marked as referenceable. FSH surfaces this as the `Characteristics: #can-be-target` keyword on the target Logical Model — the `Human` logical model in the same example is declared with `Characteristics: #can-be-target` precisely so that `FamilyMember.human` is permitted to reference it:

> "The keyword `Characteristics` MAY be used to specify the type characteristics of the logical model being defined. FSH implementations SHOULD represent these characteristics on the logical model using the [Structure Type Characteristics extension] with a code value from the [TypeCharacteristicCodes value set]."
> — [FSH Language Reference, "Defining Logical Models"](https://hl7.org/fhir/uv/shorthand/reference.html#defining-logical-models)

This repo's generator already sets `Characteristics: #can-be-target` on every generated Logical Model (see `src/test/resources/fixtures/facility/expected.fsh` line 8), so this prerequisite is already satisfied for the existing fixtures. No syntax change to `Reference(...)` is needed when nesting it under a BackboneElement path — only the path prefix changes (`col.fk1.reference` instead of `col`).

---

## Q4 — Indexed FK sub-groups (`fk1`, `fk2` as distinct named children, not slices)

**Confirmed syntax:**

```
* col.fk1 0..1 BackboneElement "Foreign key 1"
  * reference 1..1 Reference(TargetA) "Target row"
  * targetColumn 1..1 string "Target column name"
* col.fk2 0..1 BackboneElement "Foreign key 2"
  * reference 1..1 Reference(TargetB) "Target row"
  * targetColumn 1..1 string "Target column name"
```

**Conclusion:** Confirmed valid, and confirmed that **slicing (`Contains`) syntax is not merely unnecessary here — it is structurally prohibited on Logical Models in FSH**, which settles the question more strongly than "not required."

1. **Distinct named siblings need no slicing — this is literally the spec's own worked example**, applied to a BackboneElement with three differently-named children (`mother`, `father`, `sibling`), each independently typed:
   ```
   * family 0..1 BackboneElement "Family" "Members of the human's immediate family."
     * mother 0..2 FamilyMember "Mother" "..."
     * father 0..2 FamilyMember "Father" "..."
     * sibling 0..* FamilyMember "Sibling" "..."
   ```
   — [FSH Language Reference, "Defining Logical Models"](https://hl7.org/fhir/uv/shorthand/reference.html#defining-logical-models). `fk1` and `fk2` are the same shape as `mother`/`father`: fixed, distinctly-named, independently-typed sibling elements under one parent — not repeats of a single generic element.

2. **Slicing (`Contains` rules) cannot even be used on Logical Models.** Table 7 ("Relationships between FSH items and FSH rules") in the Language Reference lists three `Contains` rule variants — inline extensions, standalone extensions, and slicing — and every one of them has a **blank** cell in the `Logical` column, which the table's key defines as "prohibited":
   ```
   | Contains (inline extensions)     | ... |   |   |  (blank under Logical) |
   | Contains (standalone extensions) | ... |   |   |  (blank under Logical) |
   | Contains (slicing)               | ... |   |   |  (blank under Logical) |
   ```
   **KEY:** "Y = Rule type MAY be used, ... blank = prohibited."
   — [FSH Language Reference, Table 7](https://hl7.org/fhir/uv/shorthand/reference.html#t7)

   By contrast, `Add Element` has `Y` under `Logical` in the same table — confirming Add Element rules (the `fk1`/`fk2` named-child pattern) are the correct and only mechanism for this on a Logical Model, since `Contains`/slicing simply isn't an available rule type there at all.

So the design's choice of `fk1`/`fk2` as literal named path segments isn't just a valid alternative to slicing — for `kind = logical`, it is the *only* valid mechanism the FSH grammar makes available for "two distinct FK sub-groups on one column."

---

## Q5 — `kind = logical`-specific gotchas

Findings, each cited to the Language Reference's ["Defining Logical Models"](https://hl7.org/fhir/uv/shorthand/reference.html#defining-logical-models) section unless noted:

- **`Logical:` vs `Resource:` handling of BackboneElement children is the same mechanism.** Both item types support `Add Element` rules (Table 7, `#t7`), and the Add Element Rule section states it applies "only ... for logical models and resources" — i.e. the exact same rule type and grammar, not two different mechanisms. The repo's generated FSH already declares `Parent: Base` and uses `kind = logical` per the mapper's rendering — Add Element rules are valid there.

- **`Contains`/slicing is prohibited on Logical Models but allowed on Extensions/Profiles.** See Q4 above — this is a real behavioral difference from Profile/Extension authoring, and it's the reason the fk1/fk2-named-children design is correct rather than a stopgap.

- **Root element (parent `Parent: Base`) cardinality is not itself something FSH lets you set as a min/max on the logical model's "root":** the `Parent` keyword just picks the base type (default `Base`, or `Element` "for authors who wish to have top-level `id` and `extension` elements"). Individual added elements (like `col`) each carry their own explicit `{min}..{max}` on their Add Element rule (e.g. `0..1` for `col`, `1..1` for a required child) — there's no separate/implicit cardinality behavior for logical-model root elements beyond what's declared per-element. No spec text was found suggesting BackboneElement-typed children default to any cardinality other than what's explicitly written, or that `kind=logical` changes cardinality defaults relative to `kind=resource`.

- **`Characteristics` (e.g. `#can-be-target`) is a Logical-Model-only keyword and does not itself interact with BackboneElement children** — it is set once, at the top of the `Logical:` block, on the logical model as a whole (affecting whether *other* elements may hold a `Reference(ThisModel)` — see Q3), not per-BackboneElement-child. Nothing in the spec ties `Characteristics` values to how nested BackboneElement children are declared; they are independent concerns. `Characteristics` itself is marked Trial Use in the spec ("The use of the `Characteristics` keyword in the example above is {Trial Use}").

- **Flag rules on Logical Models exclude `MS` (must-support):** "Flag rules SHALL NOT include `MS` flags" for Logical items — irrelevant to this design (no `MS` flags are used in the proposed pattern) but worth noting as a genuine `kind=logical`-specific restriction that would trip up an unwary author reusing Profile conventions.

No ambiguity was found on any of these five points against the primary sources checked — the Language Reference is direct and unambiguous on all of Q1–Q5. fshschool.org's SUSHI documentation (`fshschool.org/docs/sushi/`) was checked and does not contain a dedicated Logical Model guide beyond pointing back to this same Language Reference; it added no independent facts.

---

## Confirmed pattern — worked example

A `patientId` column: primary key, numeric precision/scale, and two named FK sub-groups (`fk1`, `fk2`), inside a `Logical: Patient` definition — combining every confirmed rule from Q1–Q4:

```
Logical:         Patient
Id:              patient
Title:           "PATIENT"
Characteristics: #can-be-target

* patientId 1..1 BackboneElement "Patient identifier column" "The patientId SQL column"
  * isPrimaryKey 1..1 boolean "Primary key member" "True if this column is part of the table's primary key"
  * precision 1..1 integer "Numeric precision"
  * scale 0..1 integer "Numeric scale"
  * fk1 0..1 BackboneElement "Foreign key to Organization"
    * reference 1..1 Reference(Organization) "Target row" "Reference to the target Organization logical model"
    * targetColumn 1..1 string "Target column name" "Name of the referenced column on the target table"
  * fk2 0..1 BackboneElement "Foreign key to LegacyPatient"
    * reference 1..1 Reference(LegacyPatient) "Target row" "Reference to the target LegacyPatient logical model"
    * targetColumn 1..1 string "Target column name" "Name of the referenced column on the target table"
```

Every rule line in this example is a plain Add Element Rule (`* <element> {min}..{max} {flag(s)} {datatype(s)} "{short}" "{definition}"`), which is the only rule type needed — no `Contains`/slicing rules appear anywhere, consistent with Q4's finding that slicing is prohibited on `Logical` items. `Organization` and `LegacyPatient` would each need `Characteristics: #can-be-target` on their own `Logical:` definitions for the `Reference(...)` targets to be valid, per Q3.

---

## Sources consulted

- FSH Language Reference — [hl7.org/fhir/uv/shorthand/reference.html](https://hl7.org/fhir/uv/shorthand/reference.html) (release) / [build.fhir.org/ig/HL7/fhir-shorthand/reference.html](https://build.fhir.org/ig/HL7/fhir-shorthand/reference.html) (continuous build), sections: "Add Element Rules" (`#add-element-rules`), "Defining Logical Models" (`#defining-logical-models`), Table 7 "Relationships between FSH items and FSH rules" (`#t7`), "Sliced Array Paths" (`#sliced-array-paths`), "Contains Rules for Slicing" (`#contains-rules-for-slicing`).
- Canonical source markdown for the above: [github.com/HL7/fhir-shorthand `input/pagecontent/reference.md`](https://github.com/HL7/fhir-shorthand/blob/master/input/pagecontent/reference.md) (fetched raw 2026-08-23 to get complete, non-truncated verbatim text).
- FHIR R4 — [BackboneElement](https://hl7.org/fhir/R4/backboneelement.html)
- FHIR R4 — [ElementDefinition](https://hl7.org/fhir/R4/elementdefinition.html)
- fshschool.org SUSHI docs — [fshschool.org/docs/sushi/](https://fshschool.org/docs/sushi/) (checked; no independent Logical Model content beyond the Language Reference).

## Repo context consulted (not modified)

- `CONTEXT.md` — existing SQL/FHIR domain vocabulary (Primary/Foreign/Unique Key, Logical Model).
- `src/main/kotlin/fr/aphp/sashimi/mapper/StructureDefinitionMapper.kt` — current extension-based mapping (`ext-sql-is-pk`, `ext-sql-fk-columns`, `ext-sql-unique`, `ext-sql-precision`) that issue #19 proposes replacing.
- `src/test/resources/fixtures/facility/expected.fsh` — current golden fixture showing a column (`responsibleId`) with two FK targets, the closest existing analog to the `fk1`/`fk2` worked example above.
