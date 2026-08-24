package fr.aphp.sashimi

import fr.aphp.sashimi.mapper.StructureDefinitionMapper
import fr.aphp.sashimi.parser.SqlTableParser
import fr.aphp.sashimi.writer.FshWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File

/**
 * Runs the full pipeline (parse → map → write) on each fixture under
 * `src/test/resources/fixtures/<case>/` and compares the produced FSH to
 * the `expected.fsh` file by strict text equality.
 *
 * To regenerate an `expected.fsh` after an intentional output change:
 * `./gradlew run` (or the built jar) on `input.sql`, review the diff,
 * commit it intentionally. No automatic regeneration.
 */
class PipelineFixtureTest {
    private val parser = SqlTableParser()
    private val mapper = StructureDefinitionMapper()
    private val writer = FshWriter()

    private val fixturesDir = File("src/test/resources/fixtures")

    @TestFactory
    fun `pipeline produces the expected FSH for each fixture`(): List<DynamicTest> {
        val caseDirs = fixturesDir.listFiles { f -> f.isDirectory }
        check(!caseDirs.isNullOrEmpty()) {
            "No fixture found under ${fixturesDir.absolutePath} (cwd=${File(".").absolutePath}): " +
                "the folder is missing or empty, not just without results."
        }
        return caseDirs
            .sortedBy { it.name }
            .map { caseDir -> dynamicTest(caseDir.name) { assertFixture(caseDir) } }
    }

    private fun assertFixture(caseDir: File) {
        val input = File(caseDir, "input.sql").readText()
        val expected = File(caseDir, "expected.fsh").readText()

        val result = mapper.map(parser.parse(input))
        val sd = result.successes.find { it.id == caseDir.name }
        assertNotNull(sd, "No StructureDefinition with id '${caseDir.name}' produced from $caseDir/input.sql")

        val actual = writer.write(sd!!)
        assertEquals(expected, actual, "Fixture '${caseDir.name}': the generated FSH diverges from expected.fsh")
    }
}
