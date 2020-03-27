package no.nav.syfo.sykmelding.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.sql.Date
import java.sql.ResultSet
import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.Periode
import org.postgresql.util.PGobject

fun DatabaseInterface.registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmelding) {
    connection.use { connection ->

        val insertQuery: String =
                """
                    INSERT INTO egenmeldt_sykmelding (
                        id, 
                        pasientfnr, 
                        fom, 
                        tom, 
                        arbeidsforhold) 
                    VALUES (?, ?, ?, ?, ?);
                """

        var i = 1

        connection.prepareStatement(insertQuery).use {
            it.setObject(i++, egenmeldtSykmelding.id)
            it.setString(i++, egenmeldtSykmelding.fnr)
            it.setDate(i++, Date.valueOf(egenmeldtSykmelding.periode.fom))
            it.setDate(i++, Date.valueOf(egenmeldtSykmelding.periode.tom))
            it.setObject(i++, PGobject().also {
                it.type = "json"
                it.value = jacksonObjectMapper().writeValueAsString(egenmeldtSykmelding.arbeidsforhold)
            })

            it.execute()
        }

        connection.commit()
    }
}

fun DatabaseInterface.sykmeldingOverlapper(egenmeldtSykmelding: EgenmeldtSykmelding): Boolean {
    connection.use { connection ->
        val query = """
            SELECT count(*)
            FROM egenmeldt_sykmelding
            WHERE pasientfnr = ?
              AND arbeidsforhold->>'orgNummer' = ?
        """

        connection.prepareStatement(query).use {
            var i = 1
            it.setString(i++, egenmeldtSykmelding.fnr)
            it.setString(i++, egenmeldtSykmelding.arbeidsforhold?.orgNummer)
            val executeQuery = it.executeQuery()
            if (executeQuery.next()) {
                return executeQuery.getInt(1) > 0
            }
        }
    }
    return false
}

fun DatabaseInterface.sykmeldingErAlleredeRegistrert(fnr: String, periode: Periode): Boolean =
    connection.use { connection ->
        connection.prepareStatement(
            """
           SELECT 1 
           FROM egenmeldt_sykmelding 
           WHERE pasientfnr = ? 
                AND fom = ? AND tom = ?
            """
        ).use {
            it.setString(1, fnr)
            it.setDate(2, Date.valueOf(periode.fom))
            it.setDate(3, Date.valueOf(periode.tom))
            it.executeQuery().next()
        }
    }

fun DatabaseInterface.finnEgenmeldtSykmelding(id: UUID): EgenmeldtSykmelding {
    connection.use { connection ->

        val query: String =
                """
                    SELECT * 
                    FROM egenmeldt_sykmelding 
                    WHERE id = ? """

        connection.prepareStatement(query).use {
            it.setObject(1, id)
            return it.executeQuery().toList { tilEgenmeldtSykmelding() }.first()
        }
    }
}

fun DatabaseInterface.finnEgenmeldtSykmelding(pasientfnr: String): EgenmeldtSykmelding {
    connection.use { connection ->

        val query =
                """ SELECT * 
                    FROM egenmeldt_sykmelding 
                    WHERE pasientfnr = ? """

        connection.prepareStatement(query).use {
            it.setObject(1, pasientfnr)
            return it.executeQuery().toList { tilEgenmeldtSykmelding() }.first()
        }
    }
}

fun ResultSet.tilEgenmeldtSykmelding(): EgenmeldtSykmelding {
    return EgenmeldtSykmelding(
            id = getObject("id") as UUID,
            fnr = getString("pasientfnr"),
            arbeidsforhold = jacksonObjectMapper().readValue(getString("arbeidsforhold")),
            periode = Periode(
                    getDate("fom").toLocalDate(),
                    getDate("tom").toLocalDate()))
}
