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
    fun `returns non-zero exit code and logs cleanly when two CHECK constraints collide`(@TempDir tempDir: File) {
        val collidingDdl = """
            CREATE TABLE t (
                a INT,
                CHECK (a > 0),
                CHECK (a > 0)
            );
        """.trimIndent()
        val input = File(tempDir, "colliding.sql").apply { writeText(collidingDdl) }

        val exitCode = newCommand(input).call()

        assertNotEquals(0, exitCode)
        assertFalse(File(tempDir, "StructureDefinition-t.fsh").exists())
    }

    @Test
    fun `returns non-zero exit code and writes nothing when two tables normalize to the same sdId`(@TempDir tempDir: File) {
        val collidingDdl = """
            CREATE TABLE os.kern_fall (
                id UUID NOT NULL,
                CONSTRAINT pk_a PRIMARY KEY (id)
            );
            CREATE TABLE os_kern.fall (
                id UUID NOT NULL,
                CONSTRAINT pk_b PRIMARY KEY (id)
            );
        """.trimIndent()
        val input = File(tempDir, "colliding-sdid.sql").apply { writeText(collidingDdl) }

        val exitCode = newCommand(input).call()

        assertNotEquals(0, exitCode)
        assertFalse(File(tempDir, "StructureDefinition-os-kern-fall.fsh").exists())
    }

    @Test
    fun `returns non-zero exit code when two UNIQUE keys resolve to the same name`(@TempDir tempDir: File) {
        val collidingDdl = """
            CREATE TABLE t (
                code   VARCHAR(10),
                other  VARCHAR(10),
                CONSTRAINT "uq-code" UNIQUE (other),
                UNIQUE (code)
            );
        """.trimIndent()
        val input = File(tempDir, "colliding-unique.sql").apply { writeText(collidingDdl) }

        val exitCode = newCommand(input).call()

        assertNotEquals(0, exitCode)
        assertFalse(File(tempDir, "StructureDefinition-t.fsh").exists())
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
