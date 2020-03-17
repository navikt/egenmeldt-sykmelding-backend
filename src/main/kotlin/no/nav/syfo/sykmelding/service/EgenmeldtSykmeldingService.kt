package no.nav.syfo.sykmelding.service

import no.nav.syfo.db.Database
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.sykmelding.db.registrerEgenmeldtSykmelding
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest

class EgenmeldtSykmeldingService(private val database: DatabaseInterface) {
    suspend fun registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest: EgenmeldtSykmelding) {
        val fom = egenmeldtSykmeldingRequest.periode.fom
        val tom = egenmeldtSykmeldingRequest.periode.tom
        if (tom.isBefore(fom)) {
            throw TomBeforeFomDateException("Tom date is before Fom date")
        }

        database.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest)

        // todo rest

//        Ta imot forenklet sykmelding fra frontend
//        Lagre referanse og input til database
//        Lage sykmelding
//        Mappe til XML fordi infotrygd og syofoservice (se i syfosm-mottak?)
//        Legge på OK-kafka-topic
//        ???
//        Profit

    }
}
