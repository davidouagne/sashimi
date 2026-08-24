package fr.aphp.sashimi.writer

import fr.aphp.sashimi.mapper.StructureDefinitionMapper
import fr.aphp.sashimi.parser.SqlTableParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class FshWriterTest {

    private val parser = SqlTableParser()
    private val mapper = StructureDefinitionMapper()
    private val writer = FshWriter()

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun fsh(ddl: String): String =
        writer.write(mapper.map(parser.parse(ddl.trimIndent())).successes.first())

    private fun assertContains(fsh: String, vararg snippets: String) =
        snippets.forEach { assertTrue(fsh.contains(it), "FSH devrait contenir : «$it»\n\nFSH produit :\n$fsh") }

    // ── squelette de base ─────────────────────────────────────────────────

    @Nested
    inner class Header {

        @Test
        fun `contient le commentaire Sashimi`() {
            val fsh = fsh("CREATE TABLE t (id UUID NOT NULL);")
            assertContains(fsh, "Généré automatiquement par Sashimi")
        }

        @Test
        fun `keyword Logical présent`() {
            assertContains(fsh("CREATE TABLE patient (id UUID NOT NULL);"), "Logical: Patient")
        }

        @Test
        fun `Id en minuscules`() {
            assertContains(fsh("CREATE TABLE MyTable (id UUID NOT NULL);"), "Id: mytable")
        }

        @Test
        fun `table snake_case convertie en PascalCase`() {
            assertContains(fsh("CREATE TABLE care_site (id UUID NOT NULL);"), "Logical: CareSite")
        }
    }

    // ── Cardinalité ───────────────────────────────────────────────────────

    @Nested
    inner class Cardinalite {

        @Test
        fun `NOT NULL produit cardinalite 1 point 1`() {
            assertContains(fsh("CREATE TABLE t (col TEXT NOT NULL);"), "* col 1..1")
        }

        @Test
        fun `colonne nullable produit cardinalite 0 point 1`() {
            assertContains(fsh("CREATE TABLE t (col TEXT);"), "* col 0..1")
        }

        @Test
        fun `plusieurs colonnes avec cardinalites mixtes`() {
            val fsh = fsh("""
                CREATE TABLE t (
                    a TEXT NOT NULL,
                    b TEXT,
                    c INT NOT NULL
                );
            """)
            assertContains(fsh, "* a 1..1", "* b 0..1", "* c 1..1")
        }
    }

    // ── Correspondances de types SQL → FHIR ───────────────────────────────

    @Nested
    inner class TypeMapping {

        @TestFactory
        fun `types primitifs SQL vers FHIR`(): List<DynamicTest> = listOf(
            "UUID"        to "uuid",
            "TEXT"        to "string",
            "VARCHAR(50)" to "string",
            "INT"         to "integer",
            "INTEGER"     to "integer",
            "BIGINT"      to "integer",
            "NUMERIC"     to "decimal",
            "DECIMAL"     to "decimal",
            "BOOLEAN"     to "boolean",
            "DATE"        to "date",
            "TIMESTAMP"   to "dateTime",
        ).map { (sqlType, fhirType) ->
            dynamicTest("$sqlType → $fhirType") {
                val fsh = fsh("CREATE TABLE t (col $sqlType NOT NULL);")
                // Sans wrapper (colonne sans fait) ou sous ".value" (ex. INT/INTEGER/BIGINT, dont le
                // type jOOQ par défaut porte une précision non nulle même sans clause explicite dans
                // le DDL — la colonne est alors enveloppée, comme `a`/`b` dans la fixture
                // anonymous-constraints).
                assertTrue(
                    fsh.contains("* col 1..1 $fhirType") || fsh.contains("* col.value 1..1 $fhirType"),
                    "FSH devrait mapper $sqlType vers $fhirType (avec ou sans wrapper BackboneElement)\n\nFSH produit :\n$fsh",
                )
            }
        }

        @Test
        fun `colonne UUID primaire produit type uuid sous value`() {
            val fsh = fsh("""
                CREATE TABLE t (
                    id UUID NOT NULL,
                    CONSTRAINT pk_t PRIMARY KEY (id)
                );
            """)
            // PK -> colonne enveloppée : le type SQL d'origine migre sur l'enfant ".value".
            assertContains(fsh, "* id 1..1 BackboneElement", "* id.value 1..1 uuid")
        }
    }

    // ── Clé primaire ──────────────────────────────────────────────────────

    @Nested
    inner class ClePrimaire {

        private val ddl = """
            CREATE TABLE patient (
                id UUID NOT NULL,
                CONSTRAINT pk_patient PRIMARY KEY (id)
            );
        """

        @Test
        fun `colonne PK enveloppee en BackboneElement avec enfant isPrimaryKey fixe a true`() {
            assertContains(fsh(ddl),
                "* id 1..1 BackboneElement", "* id.isPrimaryKey 1..1 boolean", "* id.isPrimaryKey = true")
        }

        @Test
        fun `isPrimaryKey absent sur les colonnes non-PK`() {
            val fsh = fsh("""
                CREATE TABLE t (
                    id   UUID NOT NULL,
                    name TEXT NOT NULL,
                    CONSTRAINT pk_t PRIMARY KEY (id)
                );
            """)
            // isPrimaryKey ne doit être déclaré qu'une fois (pour id) ; name reste un élément primitif.
            assertEquals(1,
                fsh.lines().count { it.contains("isPrimaryKey 1..1 boolean") },
                "isPrimaryKey doit être déclaré exactement une fois")
            assertContains(fsh, "* name 1..1 string")
        }

        @Test
        fun `PK composite emise sur chaque colonne concernee`() {
            val fsh = fsh("""
                CREATE TABLE t (
                    col_a INT NOT NULL,
                    col_b INT NOT NULL,
                    CONSTRAINT pk_t PRIMARY KEY (col_a, col_b)
                );
            """)
            assertEquals(2, fsh.lines().count { it.contains("isPrimaryKey 1..1 boolean") },
                "PK composite : isPrimaryKey attendu sur 2 colonnes")
            assertEquals(2, fsh.lines().count { it.trim() == "* colA.isPrimaryKey = true" || it.trim() == "* colB.isPrimaryKey = true" },
                "PK composite : chaque colonne doit fixer isPrimaryKey à true")
        }
    }

    // ── Clé étrangère ─────────────────────────────────────────────────────

    @Nested
    inner class CleEtrangere {

        private val ddl = """
            CREATE TABLE encounter (
                id         UUID NOT NULL,
                patient_id UUID,
                CONSTRAINT pk_encounter  PRIMARY KEY (id),
                CONSTRAINT fk_patient    FOREIGN KEY (patient_id) REFERENCES patient(id)
            );
        """

        @Test
        fun `FK produit un type Reference`() {
            assertContains(fsh(ddl), "Reference(")
        }

        @Test
        fun `groupe fk1 emis avec reference et targetColumn fixe a la colonne cible`() {
            assertContains(fsh(ddl),
                "* patientId 0..1 BackboneElement",
                "* patientId.fk1 1..1 BackboneElement",
                "* patientId.fk1.reference 1..1 Reference(",
                "* patientId.fk1.targetColumn 1..1 string",
                "* patientId.fk1.targetColumn = \"id\"")
        }

        @Test
        fun `colonne sans FK reste un type primitif`() {
            val fsh = fsh(ddl)
            // id ne doit pas être une Reference
            val idLine = fsh.lines().first { it.trimStart().startsWith("* id ") }
            assertFalse(idLine.contains("Reference"), "id ne devrait pas être une Reference")
        }
    }

    // ── Contraintes CHECK → invariants ────────────────────────────────────

    @Nested
    inner class Invariants {

        private val ddl = """
            CREATE TABLE encounter (
                id         UUID      NOT NULL,
                start_date TIMESTAMP NOT NULL,
                note       TEXT,
                CONSTRAINT pk_encounter PRIMARY KEY (id),
                CONSTRAINT chk_note CHECK (note IS NOT NULL OR start_date IS NOT NULL)
            );
        """

        @Test
        fun `obeys emis pour chaque contrainte CHECK`() {
            assertContains(fsh(ddl), "* obeys chk-note")
        }

        @Test
        fun `bloc invariant complet genere`() {
            val fsh = fsh(ddl)
            assertContains(
                fsh,
                "Invariant: chk-note",
                "Description: \"(note IS NOT NULL OR startDate IS NOT NULL)\"",
                "Expression: \"true\"",
                "Severity: #error",
            )
        }

        @Test
        fun `plusieurs contraintes CHECK produisent plusieurs invariants`() {
            val fsh = fsh("""
                CREATE TABLE t (
                    a INT NOT NULL,
                    b INT NOT NULL,
                    CONSTRAINT chk_a CHECK (a > 0),
                    CONSTRAINT chk_b CHECK (b > 0)
                );
            """)
            assertContains(fsh, "* obeys chk-a", "* obeys chk-b")
        }
    }

    // ── Contrainte UNIQUE ─────────────────────────────────────────────────

    @Nested
    inner class ContrainteUnique {

        @Test
        fun `colonne UNIQUE enveloppee en BackboneElement avec enfant uniqueKeyName fixe au nom resolu`() {
            val fsh = fsh("""
                CREATE TABLE t (
                    code TEXT NOT NULL,
                    CONSTRAINT uq_code UNIQUE (code)
                );
            """)
            assertContains(fsh,
                "* code 1..1 BackboneElement", "* code.uniqueKeyName 1..1 string", "* code.uniqueKeyName = \"UQ_CODE\"")
        }
    }

    // ── Table de référence : cas intégral ─────────────────────────────────

    @Nested
    inner class CasIntegralDDLtoFSH {

        private val ddl = """
            CREATE TABLE encounter (
                id         UUID        NOT NULL,
                start_date TIMESTAMP   NOT NULL,
                note       TEXT,
                CONSTRAINT pk_encounter PRIMARY KEY (id),
                CONSTRAINT chk_note CHECK (note IS NOT NULL OR start_date IS NOT NULL)
            );
        """

        @Test
        fun `bloc Logical complet`() {
            val fsh = fsh(ddl)
            assertContains(fsh,
                "Logical: Encounter",
                "* id 1..1",
                "* startDate 1..1 dateTime",
                "* note 0..1 string",
                "* obeys chk-note",
                "isPrimaryKey",
            )
        }

        @Test
        fun `chaque ligne rule commence par une étoile ou est vide`() {
            val fsh = fsh(ddl)
            val ruleLines = fsh.lines()
                .filter { it.isNotBlank() }
                .filterNot { it.startsWith("//") }
                .filterNot { it.startsWith("Logical:") || it.startsWith("Parent:") ||
                  it.startsWith("Id:") || it.startsWith("Title:") ||
                  it.startsWith("Description:") || it.startsWith("Characteristics:") ||
                  it.startsWith("Invariant:") || it.startsWith("Expression:") ||
                  it.startsWith("Severity:") }
            ruleLines.forEach { line ->
                assertTrue(line.trimStart().startsWith("*"),
                    "Ligne de règle sans '*' : «$line»")
            }
        }
    }
}