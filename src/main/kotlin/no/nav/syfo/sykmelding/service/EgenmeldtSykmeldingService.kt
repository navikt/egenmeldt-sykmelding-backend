package no.nav.syfo.sykmelding.service

import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.sykmelding.db.registrerEgenmeldtSykmelding
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest

class EgenmeldtSykmeldingService(private val database: DatabaseInterface) {
    suspend fun registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmelding) {
        val fom = egenmeldtSykmelding.periode.fom
        val tom = egenmeldtSykmelding.periode.tom
        if (tom.isBefore(fom)) {
            throw TomBeforeFomDateException("Tom date is before Fom date")
        }

//        val fellesformat = fellesformatUnmarshaller.unmarshal(
//                StringReader(???)) as XMLEIFellesformat

        database.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

//        Lage sykmelding
//        Mappe til XML fordi infotrygd og syofoservice (se i syfosm-mottak?)
//        Legge på OK-kafka-topic
//        ???
//        Profit
    }

    suspend fun registrerEgenmeldtSykmelding(sykmeldingRequest: EgenmeldtSykmeldingRequest, fnr: String) {
        val sykmeldinger = sykmeldingRequest.arbeidsforhold.map {
            EgenmeldtSykmelding(UUID.randomUUID(), fnr, it, sykmeldingRequest.periode)
        }

        for (sykmelding in sykmeldinger) {
            registrerEgenmeldtSykmelding(sykmelding)
        }
    }
}
