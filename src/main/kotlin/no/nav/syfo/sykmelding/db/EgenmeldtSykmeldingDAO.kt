package no.nav.syfo.sykmelding.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.Periode
import org.postgresql.util.PGobject
import java.sql.Date
import java.sql.ResultSet

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
            it.setString(i++, egenmeldtSykmelding.fodselsnummer)
            it.setDate(i++, Date.valueOf(egenmeldtSykmelding.periode.tom))
            it.setDate(i++, Date.valueOf(egenmeldtSykmelding.periode.fom))
            it.setObject(i++, PGobject().also {
                it.type = "json"
                it.value = jacksonObjectMapper().writeValueAsString(egenmeldtSykmelding.arbeidsforhold)
            })

            it.execute()
        }

        connection.commit()
    }
}

fun DatabaseInterface.finnEgenmeldtSykmelding(id: UUID) {


    connection.use { connection ->

        val query: String =
                """
                    SELECT * 
                    FROM egenmeldt_sykmelding 
                    WHERE id = ?
                """

        connection.prepareStatement(query).use {
            it.setObject(1, id)
            it.executeQuery()
                    .toList { tilEgenmeldtSykmelding() }
        }

    }

}

fun ResultSet.tilEgenmeldtSykmelding(): EgenmeldtSykmelding {
    return EgenmeldtSykmelding(
            id = getObject("id") as UUID,
            fodselsnummer = getString("pasientfnr"),
            arbeidsforhold = jacksonObjectMapper().readValue(getString("arbeidsforhold")),
            periode = Periode(
                    getDate("fom").toLocalDate(),
                    getDate("tom").toLocalDate()))
}