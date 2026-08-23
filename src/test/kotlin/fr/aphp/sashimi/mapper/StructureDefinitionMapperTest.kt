package fr.aphp.sashimi.mapper

import fr.aphp.sashimi.parser.SqlCheckConstraint
import fr.aphp.sashimi.parser.SqlColumn
import fr.aphp.sashimi.parser.SqlTable
import fr.aphp.sashimi.parser.SqlUniqueKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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

    @Test
    fun `two anonymous CHECK constraints with identical normalized condition collide`() {
        val table = tableWithChecks(
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
        )

        val exception = assertThrows(DuplicateConstraintKeyException::class.java) { mapper.map(listOf(table)) }
        assertTrue(exception.message!!.contains("chk-a-0"), "Le message doit nommer la clé en collision : ${exception.message}")
    }

    @Test
    fun `a named CHECK colliding with an anonymous CHECK's derived key is caught`() {
        val table = tableWithChecks(
            SqlCheckConstraint(name = "chk-a-0", conditionText = "some_other_condition"),
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
        )

        val exception = assertThrows(DuplicateConstraintKeyException::class.java) { mapper.map(listOf(table)) }
        assertTrue(exception.message!!.contains("chk-a-0"), "Le message doit nommer la clé en collision : ${exception.message}")
    }

    @Test
    fun `two named CHECK constraints sharing the same explicit name collide`() {
        val table = tableWithChecks(
            SqlCheckConstraint(name = "CHK_SAME", conditionText = "a > 0"),
            SqlCheckConstraint(name = "CHK_SAME", conditionText = "b > 0"),
        )

        val exception = assertThrows(DuplicateConstraintKeyException::class.java) { mapper.map(listOf(table)) }
        assertTrue(exception.message!!.contains("chk-same"), "Le message doit nommer la clé en collision : ${exception.message}")
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

        val exception = assertThrows(DuplicateConstraintKeyException::class.java) { mapper.map(listOf(table)) }
        assertTrue(exception.message!!.contains("chk-a-0"), "Le message doit nommer la clé en collision : ${exception.message}")
    }

    @Test
    fun `distinct CHECK conditions do not collide and each keeps its own resolved key and text`() {
        val table = tableWithChecks(
            SqlCheckConstraint(name = null, conditionText = "a > 0"),
            SqlCheckConstraint(name = null, conditionText = "b > 0"),
        )

        val constraints = mapper.map(listOf(table)).single().differential.element.first().constraint
        assertEquals(listOf("chk-a-0", "chk-b-0"), constraints.map { it.key })
        assertEquals(listOf("a > 0", "b > 0"), constraints.map { it.human })
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
        // produisent le même sdId "os-kern-fall" (ticket #14).
        val tables = listOf(emptyTable("OS.KERN_FALL"), emptyTable("OS_KERN.FALL"))

        val exception = assertThrows(DuplicateStructureDefinitionIdException::class.java) { mapper.map(tables) }
        assertTrue(exception.message!!.contains("os-kern-fall"), "Le message doit nommer l'id en collision : ${exception.message}")
        assertTrue(exception.message!!.contains("OS.KERN_FALL"), "Le message doit nommer les deux tables : ${exception.message}")
        assertTrue(exception.message!!.contains("OS_KERN.FALL"), "Le message doit nommer les deux tables : ${exception.message}")
    }

    @Test
    fun `tables with distinct sdId do not collide`() {
        val tables = listOf(emptyTable("OS_KERN.FALL"), emptyTable("OS_KERN.PATIENT"))

        val structureDefinitions = mapper.map(tables)
        assertEquals(listOf("os-kern-fall", "os-kern-patient"), structureDefinitions.map { it.id })
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

        val exception = assertThrows(DuplicateUniqueKeyNameException::class.java) { mapper.map(listOf(table)) }
        assertTrue(exception.message!!.contains("uq-a-b"), "Le message doit nommer le nom en collision : ${exception.message}")
    }

    @Test
    fun `a named UNIQUE colliding with an anonymous UNIQUE's derived name is caught`() {
        val table = tableWithUniqueKeys(
            SqlUniqueKey(name = "uq-code", columns = listOf("OTHER")),
            SqlUniqueKey(name = null, columns = listOf("CODE")),
        )

        val exception = assertThrows(DuplicateUniqueKeyNameException::class.java) { mapper.map(listOf(table)) }
        assertTrue(exception.message!!.contains("uq-code"), "Le message doit nommer le nom en collision : ${exception.message}")
    }

    @Test
    fun `distinct UNIQUE keys do not collide and each column keeps its own resolved name`() {
        val table = tableWithUniqueKeys(
            SqlUniqueKey(name = null, columns = listOf("CODE")),
            SqlUniqueKey(name = null, columns = listOf("A", "B")),
        )

        val elements = mapper.map(listOf(table)).single().differential.element
        val uniqueValues = elements
            .mapNotNull { el -> el.extension.find { it.url == StructureDefinitionMapper.EXT_SQL_UNIQUE } }
            .map { (it.value as org.hl7.fhir.r4.model.StringType).value }
        assertEquals(listOf("uq-code [UNIQUE]", "uq-a-b [UNIQUE]", "uq-a-b [UNIQUE]"), uniqueValues)
    }
}
