package fr.aphp.sashimi.mapper

import fr.aphp.sashimi.parser.SqlCheckConstraint
import fr.aphp.sashimi.parser.SqlColumn
import fr.aphp.sashimi.parser.SqlTable
import fr.aphp.sashimi.parser.SqlUniqueKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cases that cannot be covered by a golden fixture (`PipelineFixtureTest` can only compare
 * pipelines that succeed): CHECK constraint key collision detection (ticket #11) makes the
 * mapping fail, so there is no "expected" FSH to compare against.
 */
class StructureDefinitionMapperTest {
    private val mapper = StructureDefinitionMapper()

    private fun tableWithChecks(vararg checks: SqlCheckConstraint) =
        SqlTable(
            name = "t",
            comment = null,
            columns = emptyList(),
            primaryKeyColumns = emptySet(),
            foreignKeys = emptyList(),
            uniqueKeys = emptyList(),
            checks = checks.toList(),
            notNullColumns = emptySet(),
        )

    private fun assertSingleFailure(
        result: MappingResult,
        expectedTableName: String,
        messageFragment: String,
    ): TableMappingFailure {
        assertTrue(result.successes.isEmpty(), "The failing table must produce no success: ${result.successes}")
        assertEquals(1, result.failures.size, "Exactly one failing table was expected: ${result.failures}")
        val failure = result.failures.single()
        assertEquals(expectedTableName, failure.tableName)
        assertTrue(
            failure.exception.message!!.contains(messageFragment),
            "The message must name the colliding key: ${failure.exception.message}",
        )
        return failure
    }

    @Test
    fun `two anonymous CHECK constraints with identical normalized condition collide`() {
        val table =
            tableWithChecks(
                SqlCheckConstraint(name = null, conditionText = "a > 0"),
                SqlCheckConstraint(name = null, conditionText = "a > 0"),
            )

        val failure = assertSingleFailure(mapper.map(listOf(table)), "t", "chk-a-0")
        assertTrue(failure.exception is DuplicateConstraintKeyException)
    }

    @Test
    fun `a named CHECK colliding with an anonymous CHECK's derived key is caught`() {
        val table =
            tableWithChecks(
                SqlCheckConstraint(name = "chk-a-0", conditionText = "some_other_condition"),
                SqlCheckConstraint(name = null, conditionText = "a > 0"),
            )

        assertSingleFailure(mapper.map(listOf(table)), "t", "chk-a-0")
    }

    @Test
    fun `two named CHECK constraints sharing the same explicit name collide`() {
        val table =
            tableWithChecks(
                SqlCheckConstraint(name = "CHK_SAME", conditionText = "a > 0"),
                SqlCheckConstraint(name = "CHK_SAME", conditionText = "b > 0"),
            )

        assertSingleFailure(mapper.map(listOf(table)), "t", "chk-same")
    }

    @Test
    fun `a collision after two already-distinct keys is still caught`() {
        // Two distinct keys processed without incident before the collision: checks that detection
        // is not limited to an adjacent pair, but covers every key seen so far.
        val table =
            tableWithChecks(
                SqlCheckConstraint(name = null, conditionText = "a > 0"),
                SqlCheckConstraint(name = null, conditionText = "b > 0"),
                SqlCheckConstraint(name = null, conditionText = "a > 0"),
            )

        assertSingleFailure(mapper.map(listOf(table)), "t", "chk-a-0")
    }

    @Test
    fun `distinct CHECK conditions do not collide and each keeps its own resolved key and text`() {
        val table =
            tableWithChecks(
                SqlCheckConstraint(name = null, conditionText = "a > 0"),
                SqlCheckConstraint(name = null, conditionText = "b > 0"),
            )

        val result = mapper.map(listOf(table))
        assertTrue(result.failures.isEmpty())
        val constraints =
            result.successes
                .single()
                .differential.element
                .first()
                .constraint
        assertEquals(listOf("chk-a-0", "chk-b-0"), constraints.map { it.key })
        assertEquals(listOf("a > 0", "b > 0"), constraints.map { it.human })
    }

    @Test
    fun `one failing table does not block the others from being mapped`() {
        // See ticket #16: the batch is no longer all-or-nothing, a failing table is skipped
        // without preventing the other tables in the same run from being mapped and returned.
        val badTable =
            tableWithChecks(
                SqlCheckConstraint(name = null, conditionText = "a > 0"),
                SqlCheckConstraint(name = null, conditionText = "a > 0"),
            )
        val goodTable = emptyTable("good")

        val result = mapper.map(listOf(badTable, goodTable))

        assertEquals(listOf("good"), result.successes.map { it.id })
        assertEquals(listOf("t"), result.failures.map { it.tableName })
        assertTrue(result.failures.single().exception is DuplicateConstraintKeyException)
    }

    private fun emptyTable(name: String) =
        SqlTable(
            name = name,
            comment = null,
            columns = emptyList(),
            primaryKeyColumns = emptySet(),
            foreignKeys = emptyList(),
            uniqueKeys = emptyList(),
            checks = emptyList(),
            notNullColumns = emptySet(),
        )

    @Test
    fun `two differently-qualified table names normalizing to the same sdId collide`() {
        // "." and "_" both normalize to "-": these two distinct qualified names produce the same
        // sdId "os-kern-fall" (ticket #14). Under partial success (#16), neither is written: the
        // collision is symmetric, with no implicit winner by order.
        val tables = listOf(emptyTable("OS.KERN_FALL"), emptyTable("OS_KERN.FALL"))

        val result = mapper.map(tables)

        assertTrue(result.successes.isEmpty(), "The two colliding tables must produce no success: ${result.successes}")
        assertEquals(setOf("OS.KERN_FALL", "OS_KERN.FALL"), result.failures.map { it.tableName }.toSet())
        result.failures.forEach { failure ->
            assertTrue(failure.exception is DuplicateStructureDefinitionIdException)
            assertTrue(
                failure.exception.message!!.contains("os-kern-fall"),
                "The message must name the colliding id: ${failure.exception.message}",
            )
            assertTrue(
                failure.exception.message!!.contains("OS.KERN_FALL"),
                "The message must name both tables: ${failure.exception.message}",
            )
            assertTrue(
                failure.exception.message!!.contains("OS_KERN.FALL"),
                "The message must name both tables: ${failure.exception.message}",
            )
        }
    }

    @Test
    fun `two tables with the exact same literal name produce a single failure entry, not a duplicate`() {
        // Spotted during code review on #16: "previousTableName == table.name" makes the "previous"
        // fallback and the "current" addition of the same name coincide — putIfAbsent must dedupe.
        val tables = listOf(emptyTable("person"), emptyTable("person"))

        val result = mapper.map(tables)

        assertTrue(result.successes.isEmpty())
        assertEquals(1, result.failures.size, "A single failure entry was expected for 'person', not a duplicate: ${result.failures}")
        assertEquals("person", result.failures.single().tableName)
    }

    @Test
    fun `a three-way sdId collision excludes all three tables, not just a pair`() {
        val tables = listOf(emptyTable("OS.KERN_FALL"), emptyTable("OS_KERN.FALL"), emptyTable("OS-KERN-FALL"))

        val result = mapper.map(tables)

        assertTrue(result.successes.isEmpty())
        assertEquals(setOf("OS.KERN_FALL", "OS_KERN.FALL", "OS-KERN-FALL"), result.failures.map { it.tableName }.toSet())
        assertEquals(3, result.failures.size, "Each table in the group must have its own entry, without duplication: ${result.failures}")
    }

    @Test
    fun `tables with distinct sdId do not collide`() {
        val tables = listOf(emptyTable("OS_KERN.FALL"), emptyTable("OS_KERN.PATIENT"))

        val result = mapper.map(tables)
        assertTrue(result.failures.isEmpty())
        assertEquals(listOf("os-kern-fall", "os-kern-patient"), result.successes.map { it.id })
    }

    private fun tableWithUniqueKeys(vararg uniqueKeys: SqlUniqueKey) =
        SqlTable(
            name = "t",
            comment = null,
            columns =
                uniqueKeys.flatMap { it.columns }.distinct().map { colName ->
                    SqlColumn(name = colName, sqlType = "INT", length = 0, precision = 0, scale = 0, nullable = true, comment = null)
                },
            primaryKeyColumns = emptySet(),
            foreignKeys = emptyList(),
            uniqueKeys = uniqueKeys.toList(),
            checks = emptyList(),
            notNullColumns = emptySet(),
        )

    @Test
    fun `two anonymous UNIQUE keys with identical resolved columns collide`() {
        val table =
            tableWithUniqueKeys(
                SqlUniqueKey(name = null, columns = listOf("A", "B")),
                SqlUniqueKey(name = null, columns = listOf("A", "B")),
            )

        val failure = assertSingleFailure(mapper.map(listOf(table)), "t", "uq-a-b")
        assertTrue(failure.exception is DuplicateUniqueKeyNameException)
    }

    @Test
    fun `a named UNIQUE colliding with an anonymous UNIQUE's derived name is caught`() {
        val table =
            tableWithUniqueKeys(
                SqlUniqueKey(name = "uq-code", columns = listOf("OTHER")),
                SqlUniqueKey(name = null, columns = listOf("CODE")),
            )

        assertSingleFailure(mapper.map(listOf(table)), "t", "uq-code")
    }

    @Test
    fun `distinct UNIQUE keys do not collide and each column gets its own uniqueKeyName child`() {
        // Name resolution (uq-code vs uq-a-b) is no longer carried by the generated FSH since the
        // extensions -> BackboneElement migration (#20/#21): only the presence of the "member of a
        // UNIQUE" fact remains observable on the ElementDefinition side, as a ".uniqueKeyName"
        // child declared alongside the wrapper. The *resolved name* collision itself is still
        // detected earlier, on the validation side (see the dedicated tests above).
        val table =
            tableWithUniqueKeys(
                SqlUniqueKey(name = null, columns = listOf("CODE")),
                SqlUniqueKey(name = null, columns = listOf("A", "B")),
            )

        val result = mapper.map(listOf(table))
        assertTrue(result.failures.isEmpty())
        val elementIds =
            result.successes
                .single()
                .differential.element
                .map { it.id }
        assertEquals(
            listOf(
                "T",
                "T.code",
                "T.code.value",
                "T.code.uniqueKeyName",
                "T.a",
                "T.a.value",
                "T.a.uniqueKeyName",
                "T.b",
                "T.b.value",
                "T.b.uniqueKeyName",
            ),
            elementIds,
        )
    }
}
