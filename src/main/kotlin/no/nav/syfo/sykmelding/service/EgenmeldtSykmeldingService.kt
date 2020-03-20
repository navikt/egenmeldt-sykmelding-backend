package no.nav.syfo.sykmelding.service

import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.sykmelding.db.registrerEgenmeldtSykmelding
import no.nav.syfo.sykmelding.db.sykmeldingOverlapper
import no.nav.syfo.sykmelding.errorhandling.exceptions.SykmeldingAlreadyExistsException
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

        if (database.sykmeldingOverlapper(egenmeldtSykmelding)) {
            throw SykmeldingAlreadyExistsException("A sykmelding with the same arbeidsgiver already exists for the given fødselsnummer")
        }

        database.registrerEgenmeldtSykmelding(egenmeldtSykmelding)
    }

    suspend fun registrerEgenmeldtSykmelding(sykmeldingRequest: EgenmeldtSykmeldingRequest, fnr: String) {
        val list = sykmeldingRequest.arbeidsforhold.map {
            EgenmeldtSykmelding(UUID.randomUUID(), fnr, it, sykmeldingRequest.periode)
        }

        for (egenmeldtSykmelding in list) {
            registrerEgenmeldtSykmelding(egenmeldtSykmelding)
        }
    }
}
