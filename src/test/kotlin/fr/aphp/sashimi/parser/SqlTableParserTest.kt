package fr.aphp.sashimi.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlTableParserTest {

    private val parser = SqlTableParser()

    @Test
    fun `parses column facts as raw DDL values`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                id UUID NOT NULL,
                note VARCHAR(255),
                amount NUMERIC(10,2)
            );
        """.trimIndent())

        assertEquals(1, tables.size)
        val t = tables.first()
        assertEquals("T", t.name)

        val id = t.columns.first { it.name == "ID" }
        assertEquals("UUID", id.sqlType)
        assertFalse(id.nullable)

        val note = t.columns.first { it.name == "NOTE" }
        assertEquals("VARCHAR", note.sqlType)
        assertEquals(255, note.length)
        assertTrue(note.nullable)

        val amount = t.columns.first { it.name == "AMOUNT" }
        assertEquals(10, amount.precision)
        assertEquals(2, amount.scale)
    }

    @Test
    fun `primary key columns are collected as a set`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                a INT NOT NULL,
                b INT NOT NULL,
                CONSTRAINT pk_t PRIMARY KEY (a, b)
            );
        """.trimIndent())

        assertEquals(setOf("A", "B"), tables.first().primaryKeyColumns)
    }

    @Test
    fun `foreign key exposes local and target columns as plain strings`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                patient_id UUID,
                CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patient(id)
            );
        """.trimIndent())

        val fk = tables.first().foreignKeys.single()
        assertEquals(listOf("PATIENT_ID"), fk.localColumns)
        assertEquals("PATIENT", fk.targetTable)
        assertEquals(listOf("ID"), fk.targetColumns)
    }

    @Test
    fun `unique key exposes its name and columns`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                code VARCHAR(10),
                CONSTRAINT uq_code UNIQUE (code)
            );
        """.trimIndent())

        val uk = tables.first().uniqueKeys.single()
        assertEquals("UQ_CODE", uk.name)
        assertEquals(listOf("CODE"), uk.columns)
    }

    @Test
    fun `check IS NOT NULL forces the column into notNullColumns without touching nullable`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                note VARCHAR(10),
                CONSTRAINT chk_note CHECK (note IS NOT NULL)
            );
        """.trimIndent())

        val t = tables.first()
        assertEquals(setOf("NOTE"), t.notNullColumns)
        assertTrue(t.columns.single().nullable, "SqlColumn.nullable doit rester un fait DDL pur")
    }

    @Test
    fun `check condition text is rendered dialect-faithfully, not raw toString`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                some_date DATE,
                CONSTRAINT c1 CHECK (TRUNC(some_date, 'MM') = some_date)
            );
        """.trimIndent())

        val check = tables.first().checks.single()
        assertEquals("C1", check.name)
        assertEquals("date_trunc('month', SOME_DATE) = SOME_DATE", check.conditionText)
    }

    @Test
    fun `table and column comments are attached`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                note VARCHAR(10)
            );
            COMMENT ON TABLE t IS 'Table comment';
            COMMENT ON COLUMN t.note IS 'Column comment';
        """.trimIndent())

        val t = tables.first()
        assertEquals("Table comment", t.comment)
        assertEquals("Column comment", t.columns.single().comment)
    }

    @Test
    fun `checks list is unfiltered - IS NOT NULL checks also appear as checks`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                note VARCHAR(10),
                CONSTRAINT chk_note CHECK (note IS NOT NULL)
            );
        """.trimIndent())

        assertEquals(1, tables.first().checks.size)
    }

    @Test
    fun `anonymous CHECK has a null name, not an empty string`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                a INT,
                CHECK (a > 0)
            );
        """.trimIndent())

        assertNull(tables.first().checks.single().name)
    }

    @Test
    fun `anonymous UNIQUE has a null name, not an empty string`() {
        val tables = parser.parse("""
            CREATE TABLE t (
                code VARCHAR(10) UNIQUE
            );
        """.trimIndent())

        assertNull(tables.first().uniqueKeys.single().name)
    }
}
