package fr.aphp.sashimi.mapper

import fr.aphp.sashimi.parser.SqlTableParser
import org.jooq.impl.QOM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InvariantTextTest {

    private val parser = SqlTableParser()

    private fun renderFirstCheck(ddl: String): String {
        val queries = parser.parse(ddl)
        val check = queries.filterIsInstance<QOM.CreateTable>()
            .single()
            .`$tableElements`()
            .filterIsInstance<QOM.Check>()
            .single()
        return InvariantText.render(check.`$condition`())
    }

    @Test
    fun `uppercases SQL keywords and camelCases identifiers`() {
        val ddl = """
            CREATE TABLE t (
                note VARCHAR(10),
                start_date DATE,
                CONSTRAINT c1 CHECK (note IS NOT NULL OR start_date IS NOT NULL)
            )
        """.trimIndent()

        assertEquals("(note IS NOT NULL OR startDate IS NOT NULL)", renderFirstCheck(ddl))
    }

    @Test
    fun `preserves the case of string literals`() {
        val ddl = """
            CREATE TABLE t (
                flag CHAR(1),
                CONSTRAINT c1 CHECK (flag = 'J')
            )
        """.trimIndent()

        assertEquals("flag = 'J'", renderFirstCheck(ddl))
    }

    @Test
    fun `preserves the case of literals in an IN list`() {
        val ddl = """
            CREATE TABLE t (
                code VARCHAR(1),
                CONSTRAINT c1 CHECK (code IN ('B', 'U'))
            )
        """.trimIndent()

        assertEquals("code IN ('B', 'U')", renderFirstCheck(ddl))
    }

    @Test
    fun `renders single-argument TRUNC as a faithful day truncation`() {
        val ddl = """
            CREATE TABLE t (
                some_date DATE,
                CONSTRAINT c1 CHECK (TRUNC(some_date) = some_date)
            )
        """.trimIndent()

        assertEquals("date_trunc('day', someDate) = someDate", renderFirstCheck(ddl))
    }

    @Test
    fun `renders TRUNC with an explicit format instead of silently dropping it`() {
        val ddl = """
            CREATE TABLE t (
                some_date DATE,
                CONSTRAINT c1 CHECK (TRUNC(some_date, 'MM') = some_date)
            )
        """.trimIndent()

        assertEquals("date_trunc('month', someDate) = someDate", renderFirstCheck(ddl))
    }
}
