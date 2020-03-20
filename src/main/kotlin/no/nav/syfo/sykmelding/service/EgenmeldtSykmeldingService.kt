package no.nav.syfo.sykmelding.service

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.sykmelding.db.registrerEgenmeldtSykmelding
import no.nav.syfo.sykmelding.db.sykmeldingOverlapper
import no.nav.syfo.sykmelding.errorhandling.exceptions.SykmeldingAlreadyExistsException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.mapping.opprettFellesformat
import no.nav.syfo.sykmelding.mapping.toSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Pasient
import no.nav.syfo.sykmelding.util.extractHelseOpplysningerArbeidsuforhet
import no.nav.syfo.sykmelding.util.fellesformatMarshaller
import no.nav.syfo.sykmelding.util.get
import no.nav.syfo.sykmelding.util.toString

class EgenmeldtSykmeldingService(private val oppdaterTopicsService: OppdaterTopicsService, private val database: DatabaseInterface) {
    val dummyTssIdent = "80000821845"

    fun registrerEgenmeldtSykmelding(sykmeldingRequest: EgenmeldtSykmeldingRequest, fnr: String) {
        val list = sykmeldingRequest.arbeidsforhold.map {
            EgenmeldtSykmelding(UUID.randomUUID(), fnr, it, sykmeldingRequest.periode)
        }
        for (egenmeldtSykmelding in list) {
            registrerEgenmeldtSykmelding(egenmeldtSykmelding, fnr)
        }
    }

    private fun registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmelding, fnr: String) {
        val fom = egenmeldtSykmelding.periode.fom
        val tom = egenmeldtSykmelding.periode.tom
        if (tom.isBefore(fom)) {
            throw TomBeforeFomDateException("Tom date is before Fom date")
        }

        if (database.sykmeldingOverlapper(egenmeldtSykmelding)) {
            throw SykmeldingAlreadyExistsException("A sykmelding with the same arbeidsgiver already exists for the given fødselsnummer")
        }
        database.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

        val sykmeldingId = egenmeldtSykmelding.id.toString()
        val pasient = Pasient(
            fnr = fnr,
            aktorId = "1826914851343",
            fornavn = "Fanny",
            mellomnavn = null,
            etternavn = "Storm")
        val fellesformat = opprettFellesformat(sykmeldt = pasient, sykmeldingId = sykmeldingId)
        val healthInformation = extractHelseOpplysningerArbeidsuforhet(fellesformat)
        val msgHead = fellesformat.get<XMLMsgHead>()

        val sykmelding = healthInformation.toSykmelding(
            sykmeldingId = sykmeldingId,
            pasientAktoerId = pasient.aktorId,
            legeAktoerId = pasient.aktorId,
            msgId = sykmeldingId,
            signaturDato = msgHead.msgInfo.genDate
        )

        val receivedSykmelding = ReceivedSykmelding(
            sykmelding = sykmelding,
            personNrPasient = fnr,
            tlfPasient = healthInformation.pasient.kontaktInfo.firstOrNull()?.teleAddress?.v,
            personNrLege = pasient.fnr,
            navLogId = sykmeldingId,
            msgId = sykmeldingId,
            legekontorOrgNr = null,
            legekontorOrgName = "",
            legekontorHerId = null,
            legekontorReshId = null,
            mottattDato = LocalDateTime.now(),
            rulesetVersion = healthInformation.regelSettVersjon,
            fellesformat = fellesformatMarshaller.toString(fellesformat),
            tssid = dummyTssIdent
        )
        oppdaterTopicsService.oppdaterTopics(receivedSykmelding)
    }
}
