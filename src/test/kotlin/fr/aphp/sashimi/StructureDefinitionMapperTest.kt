package fr.aphp.sashimi

import fr.aphp.sashimi.mapper.StructureDefinitionMapper
import fr.aphp.sashimi.parser.SqlTableParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StructureDefinitionMapperTest {

    private val parser = SqlTableParser()
    private val mapper = StructureDefinitionMapper()

    private val simpleDdl = """
        CREATE TABLE patient_record (
            id         UUID        NOT NULL,
            last_name  VARCHAR(255) NOT NULL,
            birth_date DATE,
            active     BOOLEAN,
            CONSTRAINT pk_patient PRIMARY KEY (id)
        );
    """.trimIndent()

    @Test
    fun `map simple table from parsed DDL`() {
        val tables = parser.parse(simpleDdl)
        assertEquals(1, tables.size)

        val sd = mapper.map(tables).first()
        assertEquals("PatientRecord", sd.name)

        // root + 4 colonnes
        assertEquals(5, sd.differential.element.size)

        val idEl = sd.differential.element.find { it.path == "PatientRecord.id" }
        assertNotNull(idEl)
        assertEquals(1, idEl!!.min)         // NOT NULL → min=1

        val birthEl = sd.differential.element.find { it.path == "PatientRecord.birthDate" }
        assertNotNull(birthEl)
        assertEquals("date", birthEl!!.typeFirstRep.code)
        assertEquals(0, birthEl.min)        // nullable
    }
}
