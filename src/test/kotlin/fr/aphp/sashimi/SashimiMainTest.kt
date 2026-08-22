package fr.aphp.sashimi

import fr.aphp.sashimi.mapper.StructureDefinitionMapper
import fr.aphp.sashimi.parser.SqlTableParser
import fr.aphp.sashimi.writer.FshWriter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SashimiMainTest {

    private fun newCommand(input: File, output: String = "", dialect: String = "DEFAULT"): TranscribeCommand {
        val command = TranscribeCommand()
        command.sqlTableParser = SqlTableParser()
        command.structureDefinitionMapper = StructureDefinitionMapper()
        command.fshWriter = FshWriter()
        command.input = input
        command.output = output
        command.dialect = dialect
        return command
    }

    private val simpleDdl = """
        CREATE TABLE patient_record (
            id         UUID        NOT NULL,
            last_name  VARCHAR(255) NOT NULL,
            CONSTRAINT pk_patient PRIMARY KEY (id)
        );
    """.trimIndent()

    @Test
    fun `returns non-zero exit code when input file is missing`(@TempDir tempDir: File) {
        val missing = File(tempDir, "does-not-exist.sql")

        val exitCode = newCommand(missing).call()

        assertNotEquals(0, exitCode)
    }

    @Test
    fun `returns non-zero exit code when no StructureDefinition is produced`(@TempDir tempDir: File) {
        val emptyDdl = File(tempDir, "empty.sql").apply { writeText("-- no CREATE TABLE here\n") }

        val exitCode = newCommand(emptyDdl).call()

        assertNotEquals(0, exitCode)
    }

    @Test
    fun `returns zero and writes to the input file's directory by default`(@TempDir tempDir: File) {
        val input = File(tempDir, "schema.sql").apply { writeText(simpleDdl) }

        val exitCode = newCommand(input).call()

        assertEquals(0, exitCode)
        assertTrue(File(tempDir, "StructureDefinition-patient-record.fsh").exists())
    }

    @Test
    fun `writes to the explicit output directory when provided`(@TempDir tempDir: File) {
        val input = File(tempDir, "schema.sql").apply { writeText(simpleDdl) }
        val outputDir = File(tempDir, "out").apply { mkdirs() }

        val exitCode = newCommand(input, output = outputDir.absolutePath).call()

        assertEquals(0, exitCode)
        assertTrue(File(outputDir, "StructureDefinition-patient-record.fsh").exists())
        assertFalse(File(tempDir, "StructureDefinition-patient-record.fsh").exists())
    }
}
