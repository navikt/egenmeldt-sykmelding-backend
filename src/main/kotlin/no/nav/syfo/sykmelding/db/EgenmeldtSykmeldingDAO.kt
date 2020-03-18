package no.nav.syfo.sykmelding.db

import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding

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
            it.setObject(i++, egenmeldtSykmelding.periode.fom)
            it.setObject(i++, egenmeldtSykmelding.periode.tom)
            it.setObject(i++, egenmeldtSykmelding.arbeidsforhold)
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

        var i = 1

        connection.prepareStatement(query).use {
            it.setObject(i++, id)
        }
        // TODO: Lag objekt og returner

        TODO("Må implementeres")
    }
}
