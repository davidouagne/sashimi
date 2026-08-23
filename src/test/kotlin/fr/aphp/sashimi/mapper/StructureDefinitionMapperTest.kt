package fr.aphp.sashimi.mapper

import fr.aphp.sashimi.parser.SqlCheckConstraint
import fr.aphp.sashimi.parser.SqlColumn
import fr.aphp.sashimi.parser.SqlTable
import fr.aphp.sashimi.parser.SqlUniqueKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cas non couvrables par une fixture dorée (`PipelineFixtureTest` ne sait comparer que des
 * pipelines qui aboutissent) : la détection de collision de clé de contrainte CHECK
 * (ticket #11) fait échouer le mapping, elle n'a pas de FSH "attendu" à comparer.
 */
class StructureDefinitionMapperTest {

    private val mapper = StructureDefinitionMapper()

    private fun tableWithChecks(vararg checks: SqlCheckConstraint) = SqlTable(
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
        assertTrue(result.successes.isEmpty(), "La table en échec ne doit produire aucun succès : ${result.successes}")
        assertEquals(1, result.failures.size, "Une seule table en échec était attendue : ${result.failures}")
        val failure = result.failures.single()
        assertEquals(expectedTableName, failure.tableName)
        assertTrue(
            failure.exception.message!!.contains(messageFragment),
            "Le message doit nommer la clé en collision : ${failure.exception.message}",
        )
        return failure
    }

    @Test
    fun `two anonymous CHECK constraints with identical normalized condition collide`() {
        val table = tableWithChecks(
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
        )

        val failure = assertSingleFailure(mapper.map(listOf(table)), "t", "chk-a-0")
        assertTrue(failure.exception is DuplicateConstraintKeyException)
    }

    @Test
    fun `a named CHECK colliding with an anonymous CHECK's derived key is caught`() {
        val table = tableWithChecks(
            SqlCheckConstraint(name = "chk-a-0", conditionText = "some_other_condition"),
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
        )

        assertSingleFailure(mapper.map(listOf(table)), "t", "chk-a-0")
    }

    @Test
    fun `two named CHECK constraints sharing the same explicit name collide`() {
        val table = tableWithChecks(
            SqlCheckConstraint(name = "CHK_SAME", conditionText = "a > 0"),
            SqlCheckConstraint(name = "CHK_SAME", conditionText = "b > 0"),
        )

        assertSingleFailure(mapper.map(listOf(table)), "t", "chk-same")
    }

    @Test
    fun `a collision after two already-distinct keys is still caught`() {
        // Deux clés distinctes traitées sans accroc avant la collision : vérifie que la détection
        // n'est pas limitée à une paire adjacente, mais porte sur toutes les clés vues jusqu'ici.
        val table = tableWithChecks(
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
            SqlCheckConstraint(name = null, conditionText = "b > 0"),
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
        )

        assertSingleFailure(mapper.map(listOf(table)), "t", "chk-a-0")
    }

    @Test
    fun `distinct CHECK conditions do not collide and each keeps its own resolved key and text`() {
        val table = tableWithChecks(
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
            SqlCheckConstraint(name = null, conditionText = "b > 0"),
        )

        val result = mapper.map(listOf(table))
        assertTrue(result.failures.isEmpty())
        val constraints = result.successes.single().differential.element.first().constraint
        assertEquals(listOf("chk-a-0", "chk-b-0"), constraints.map { it.key })
        assertEquals(listOf("a > 0", "b > 0"), constraints.map { it.human })
    }

    @Test
    fun `one failing table does not block the others from being mapped`() {
        // Cf. ticket #16 : le batch n'est plus tout-ou-rien, une table en échec est ignorée
        // sans empêcher les autres tables du même run d'être mappées et retournées.
        val badTable = tableWithChecks(
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
        )
        val goodTable = emptyTable("good")

        val result = mapper.map(listOf(badTable, goodTable))

        assertEquals(listOf("good"), result.successes.map { it.id })
        assertEquals(listOf("t"), result.failures.map { it.tableName })
        assertTrue(result.failures.single().exception is DuplicateConstraintKeyException)
    }

    private fun emptyTable(name: String) = SqlTable(
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
        // "." et "_" se normalisent tous deux vers "-" : ces deux noms qualifiés distincts
        // produisent le même sdId "os-kern-fall" (ticket #14). Sous succès partiel (#16), ni l'une
        // ni l'autre n'est écrite : la collision est symétrique, pas de gagnant implicite par ordre.
        val tables = listOf(emptyTable("OS.KERN_FALL"), emptyTable("OS_KERN.FALL"))

        val result = mapper.map(tables)

        assertTrue(result.successes.isEmpty(), "Les deux tables en collision ne doivent produire aucun succès : ${result.successes}")
        assertEquals(setOf("OS.KERN_FALL", "OS_KERN.FALL"), result.failures.map { it.tableName }.toSet())
        result.failures.forEach { failure ->
            assertTrue(failure.exception is DuplicateStructureDefinitionIdException)
            assertTrue(failure.exception.message!!.contains("os-kern-fall"), "Le message doit nommer l'id en collision : ${failure.exception.message}")
            assertTrue(failure.exception.message!!.contains("OS.KERN_FALL"), "Le message doit nommer les deux tables : ${failure.exception.message}")
            assertTrue(failure.exception.message!!.contains("OS_KERN.FALL"), "Le message doit nommer les deux tables : ${failure.exception.message}")
        }
    }

    @Test
    fun `two tables with the exact same literal name produce a single failure entry, not a duplicate`() {
        // Repéré par code-review sur #16 : "previousTableName == table.name" fait coïncider le
        // repli "previous" et l'ajout "current" du même nom — putIfAbsent doit dédupliquer.
        val tables = listOf(emptyTable("person"), emptyTable("person"))

        val result = mapper.map(tables)

        assertTrue(result.successes.isEmpty())
        assertEquals(1, result.failures.size, "Une seule entrée d'échec attendue pour 'person', pas un doublon : ${result.failures}")
        assertEquals("person", result.failures.single().tableName)
    }

    @Test
    fun `a three-way sdId collision excludes all three tables, not just a pair`() {
        val tables = listOf(emptyTable("OS.KERN_FALL"), emptyTable("OS_KERN.FALL"), emptyTable("OS-KERN-FALL"))

        val result = mapper.map(tables)

        assertTrue(result.successes.isEmpty())
        assertEquals(setOf("OS.KERN_FALL", "OS_KERN.FALL", "OS-KERN-FALL"), result.failures.map { it.tableName }.toSet())
        assertEquals(3, result.failures.size, "Chaque table du groupe doit avoir sa propre entrée, sans doublon : ${result.failures}")
    }

    @Test
    fun `tables with distinct sdId do not collide`() {
        val tables = listOf(emptyTable("OS_KERN.FALL"), emptyTable("OS_KERN.PATIENT"))

        val result = mapper.map(tables)
        assertTrue(result.failures.isEmpty())
        assertEquals(listOf("os-kern-fall", "os-kern-patient"), result.successes.map { it.id })
    }

    private fun tableWithUniqueKeys(vararg uniqueKeys: SqlUniqueKey) = SqlTable(
        name = "t",
        comment = null,
        columns = uniqueKeys.flatMap { it.columns }.distinct().map { colName ->
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
        val table = tableWithUniqueKeys(
            SqlUniqueKey(name = null, columns = listOf("A", "B")),
            SqlUniqueKey(name = null, columns = listOf("A", "B")),
        )

        val failure = assertSingleFailure(mapper.map(listOf(table)), "t", "uq-a-b")
        assertTrue(failure.exception is DuplicateUniqueKeyNameException)
    }

    @Test
    fun `a named UNIQUE colliding with an anonymous UNIQUE's derived name is caught`() {
        val table = tableWithUniqueKeys(
            SqlUniqueKey(name = "uq-code", columns = listOf("OTHER")),
            SqlUniqueKey(name = null, columns = listOf("CODE")),
        )

        assertSingleFailure(mapper.map(listOf(table)), "t", "uq-code")
    }

    @Test
    fun `distinct UNIQUE keys do not collide and each column keeps its own resolved name`() {
        val table = tableWithUniqueKeys(
            SqlUniqueKey(name = null, columns = listOf("CODE")),
            SqlUniqueKey(name = null, columns = listOf("A", "B")),
        )

        val result = mapper.map(listOf(table))
        assertTrue(result.failures.isEmpty())
        val elements = result.successes.single().differential.element
        val uniqueValues = elements
            .mapNotNull { el -> el.extension.find { it.url == StructureDefinitionMapper.EXT_SQL_UNIQUE } }
            .map { (it.value as org.hl7.fhir.r4.model.StringType).value }
        assertEquals(listOf("uq-code [UNIQUE]", "uq-a-b [UNIQUE]", "uq-a-b [UNIQUE]"), uniqueValues)
    }
}
