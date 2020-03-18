package no.nav.syfo.sykmelding.db

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest

fun DatabaseInterface.registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmeldingRequest) {
    connection.use { connection ->

        val insertQuery : String =
                """
                    INSERT INTO egenmeldt_sykmelding (
                        id, 
                        pasientfnr, 
                        from_date, 
                        to_date, 
                        receivedsykmelding) 
                    VALUES (?, ?, ?, ?, ?);
                """
        connection.prepareStatement(insertQuery).use {

        }
//        connection.registerStatus(sykmeldingStatusEvent)
//        connection.commit()
    }
}

