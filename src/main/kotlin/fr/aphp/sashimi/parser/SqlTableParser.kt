package fr.aphp.sashimi.parser

import jakarta.enterprise.context.ApplicationScoped
import org.jooq.Query
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory

/**
 * Parse un fichier SQL (DDL ou DML) avec le parser jOOQ et retourne la liste
 * des [Query] produites.
 *
 * On utilise [org.jooq.DSLContext.parser] pour analyser le SQL brut sans
 * connexion à une base de données. Le dialecte cible peut être précisé pour
 * affiner la reconnaissance de la syntaxe (ex. `POSTGRES`, `MYSQL`…) ;
 * en cas de dialecte inconnu, `DEFAULT` est utilisé en repli.
 *
 * @param sql         Contenu SQL à parser (une ou plusieurs instructions séparées par `;`).
 * @param dialectName Nom du dialecte jOOQ (insensible à la casse). Valeur par défaut : `"DEFAULT"`.
 * @return Liste des [Query] parsées, ou liste vide en cas d'erreur de parsing.
 */
@ApplicationScoped
class SqlTableParser {

    private val log = LoggerFactory.getLogger(SqlTableParser::class.java)

    fun parse(sql: String, dialectName: String = "DEFAULT"): List<Query> {
        val dialect = runCatching { SQLDialect.valueOf(dialectName.uppercase()) }
            .getOrElse {
                log.warn("Dialecte '$dialectName' inconnu, utilisation de DEFAULT")
                SQLDialect.DEFAULT
            }

        val queries = try {
            DSL.using(dialect).parser().parse(sql)
        } catch (e: Exception) {
            log.error("Erreur de parsing SQL : ${e.message}")
            return emptyList()
        }

        log.debug("${queries.queries().size} requête(s) parsée(s) via jOOQ")
        return queries.`$queries`()
    }
}