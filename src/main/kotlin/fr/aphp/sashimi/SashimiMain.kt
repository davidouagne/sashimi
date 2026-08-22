package fr.aphp.sashimi

import fr.aphp.sashimi.mapper.StructureDefinitionMapper
import fr.aphp.sashimi.parser.SqlTableParser
import fr.aphp.sashimi.writer.FshWriter
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.File
import java.util.concurrent.Callable

@QuarkusMain
class SashimiMain : QuarkusApplication {
  @Inject
  lateinit var factory: CommandLine.IFactory

  override fun run(vararg args: String): Int {
    return CommandLine(SashimiCommand::class.java, factory).execute(*args)
  }
}

// Commande racine
@CommandLine.Command(
  name = "sashimi",
  mixinStandardHelpOptions = true,
  version = ["1.0.0"],
  subcommands = [TranscribeCommand::class]
)
class SashimiCommand

@CommandLine.Command(name = "transcribe", description = ["Transcribe a SQL DDL file into FHIR StructureDefinitions in FSH format."])
class TranscribeCommand : Callable<Int> {

  private val log = LoggerFactory.getLogger(TranscribeCommand::class.java)

  @CommandLine.Option(names = ["-i", "--input"],
    required = true,
    description = ["SQL DDL input file to transcribe (CREATE TABLE statements)"])
  lateinit var input: File

  @CommandLine.Option(names = ["-o", "--output"],
    description = ["Output directory for generated .fsh files (default: current directory)"],
    defaultValue = "")
  lateinit var output: String

  @CommandLine.Option(names = ["--dialect"],
    description = ["SQL dialect for DDL parsing: POSTGRES | MYSQL | DEFAULT"],
    defaultValue = "DEFAULT")
  lateinit var dialect: String

  @CommandLine.Option(names = ["-h", "--help", "-?", "-help"], usageHelp = true, description = [
    "Usage: sashimi transcribe [OPTIONS]\n" +
      "\n" +
      "  Transcribe a SQL DDL file into FHIR StructureDefinitions in FSH format.\n" +
      "  Parses CREATE TABLE statements and maps columns and foreign\n" +
      "  keys to FHIR element definitions.\n" +
      "\n" +
      "Options:\n" +
      "  -i, --input FILE                SQL DDL input file to transcribe\n" +
      "                                  (CREATE TABLE statements).  [required]\n" +
      "  -o, --output DIR                Output directory for generated .fsh files.\n" +
      "                                  [default: current directory]\n" +
      "  --dialect DIALECT               SQL dialect for DDL parsing.\n" +
      "                                  Supported values: POSTGRES | MYSQL | DEFAULT.\n" +
      "                                  [default: DEFAULT]\n" +
      "  -h, --help, -?, -help           Show this message and exit.\n" +
      "\n" +
      "Examples:\n" +
      "  sashimi transcribe -i schema.sql\n" +
      "  sashimi transcribe -i schema.sql -o src/main/fsh --dialect POSTGRES\n"
  ])
  var help: Boolean = false

  @Inject lateinit var sqlTableParser: SqlTableParser        // jOOQ
  @Inject lateinit var structureDefinitionMapper: StructureDefinitionMapper
  @Inject lateinit var fshWriter: FshWriter           // ton writer existant

  override fun call(): Int {
    if (!input.exists()) {
      log.error("Fichier introuvable : $input")
      return 1
    }

    log.info("Parsing SQL : $input (dialect=$dialect)")
    val queries = sqlTableParser.parse(input.readText(), dialect)
    log.info("${queries.size} requête(s) détectée(s)")

    val structureDefinitions = structureDefinitionMapper.map(queries)

    if (structureDefinitions.isEmpty()) {
      log.error("Aucun StructureDefinition généré à partir de $input")
      return 1
    }

    val outputDir = output.ifEmpty {
      input.absoluteFile.parentFile.absolutePath  // absoluteFile résout le chemin relatif d'abord
    }

    structureDefinitions.forEach { sd ->
      val fsh = fshWriter.write(sd)

      val outputFile = "${outputDir + File.separator}StructureDefinition-${sd.id}.fsh"
      File(outputFile).writeText(fsh)
      log.info("FSH généré : $outputFile")
    }

    return 0
  }
}