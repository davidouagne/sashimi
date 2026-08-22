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
 * Fait tourner le pipeline complet (parse → map → write) sur chaque fixture
 * de `src/test/resources/fixtures/<cas>/` et compare le FSH produit au
 * fichier `expected.fsh` par égalité stricte du texte.
 *
 * Pour régénérer un `expected.fsh` après un changement intentionnel de
 * sortie : `./gradlew run` (ou le jar buildé) sur `input.sql`, relire le
 * diff, committer intentionnellement. Pas de régénération automatique.
 */
class PipelineFixtureTest {

    private val parser = SqlTableParser()
    private val mapper = StructureDefinitionMapper()
    private val writer = FshWriter()

    private val fixturesDir = File("src/test/resources/fixtures")

    @TestFactory
    fun `pipeline produces the expected FSH for each fixture`(): List<DynamicTest> =
        fixturesDir.listFiles { f -> f.isDirectory }
            .orEmpty()
            .sortedBy { it.name }
            .map { caseDir -> dynamicTest(caseDir.name) { assertFixture(caseDir) } }

    private fun assertFixture(caseDir: File) {
        val input = File(caseDir, "input.sql").readText()
        val expected = File(caseDir, "expected.fsh").readText()

        val structureDefinitions = mapper.map(parser.parse(input))
        val sd = structureDefinitions.find { it.id == caseDir.name }
        assertNotNull(sd, "Aucun StructureDefinition d'id '${caseDir.name}' produit à partir de ${caseDir}/input.sql")

        val actual = writer.write(sd!!)
        assertEquals(expected, actual, "Fixture '${caseDir.name}' : le FSH généré diverge de expected.fsh")
    }
}
