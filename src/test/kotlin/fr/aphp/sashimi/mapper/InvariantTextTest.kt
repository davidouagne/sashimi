package fr.aphp.sashimi.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InvariantTextTest {

    @Test
    fun `uppercases SQL keywords and camelCases identifiers`() {
        assertEquals(
            "(note IS NOT NULL OR startDate IS NOT NULL)",
            InvariantText.normalize("(note IS NOT NULL OR start_date IS NOT NULL)"),
        )
    }

    @Test
    fun `preserves the case of string literals`() {
        assertEquals(
            "flag = 'J'",
            InvariantText.normalize("flag = 'J'"),
        )
    }

    @Test
    fun `preserves the case of literals in an IN list`() {
        assertEquals(
            "code IN ('B', 'U')",
            InvariantText.normalize("code in ('B', 'U')"),
        )
    }

    @Test
    fun `leaves a SQL function call untouched, only the identifier is camelCased`() {
        assertEquals(
            "date_trunc('day', someDate) = someDate",
            InvariantText.normalize("date_trunc('day', some_date) = some_date"),
        )
    }

    @Test
    fun `collapses irregular whitespace and parenthesis spacing`() {
        assertEquals(
            "(a = 1 AND b = 2)",
            InvariantText.normalize("(  a  =  1   and\nb = 2 )"),
        )
    }
}
