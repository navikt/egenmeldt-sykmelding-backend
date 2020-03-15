package no.nav.syfo.sykmelding.service

import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest

class EgenmeldtSykmeldingService {
    suspend fun registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest: EgenmeldtSykmeldingRequest) {
        val fom = egenmeldtSykmeldingRequest.periode.fom
        val tom = egenmeldtSykmeldingRequest.periode.tom
        if (tom.isBefore(fom)) {
            throw TomBeforeFomDateException("Tom date is before Fom date")
        }
        // todo rest
    }
}
