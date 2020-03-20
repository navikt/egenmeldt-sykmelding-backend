package no.nav.syfo.sykmelding.service

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.objectMapper
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.mapping.mapOcrFilTilFellesformat
import no.nav.syfo.sykmelding.mapping.toSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Pasient
import no.nav.syfo.sykmelding.util.extractHelseOpplysningerArbeidsuforhet
import no.nav.syfo.sykmelding.util.get

class EgenmeldtSykmeldingService(private val oppdaterTopicsService: OppdaterTopicsService) {
    val dummyTssIdent = "80000821845"

    suspend fun registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest: EgenmeldtSykmeldingRequest, fnr: String) {
        val fom = egenmeldtSykmeldingRequest.periode.fom
        val tom = egenmeldtSykmeldingRequest.periode.tom
        if (tom.isBefore(fom)) {
            throw TomBeforeFomDateException("Tom date is before Fom date")
        }
        val sykmeldingId = UUID.randomUUID().toString()
        val pasient = Pasient(
            fnr = fnr,
            aktorId = "1826914851343",
            fornavn = "Fanny",
            mellomnavn = null,
            etternavn = "Storm")
        val fellesformat = mapOcrFilTilFellesformat(sykmeldt = pasient, sykmeldingId = sykmeldingId)
        val healthInformation = extractHelseOpplysningerArbeidsuforhet(fellesformat)
        val msgHead = fellesformat.get<XMLMsgHead>()

        val sykmelding = healthInformation.toSykmelding(
            sykmeldingId = UUID.randomUUID().toString(),
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
            fellesformat = objectMapper.writeValueAsString(fellesformat),
            tssid = dummyTssIdent
        )
        oppdaterTopicsService.oppdaterTopics(receivedSykmelding)
    }
}
