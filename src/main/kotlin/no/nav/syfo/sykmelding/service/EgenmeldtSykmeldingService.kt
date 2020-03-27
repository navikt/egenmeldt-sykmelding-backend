package no.nav.syfo.sykmelding.service

import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDate
import java.util.UUID
import javax.jms.MessageProducer
import javax.jms.Session
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.log
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_ALREADY_EXISTS_COUNTER
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_COUNTER
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_ERROR_TOM_BEFORE_FOM_COUNTER
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.syfosmregister.client.SyfosmregisterSykmeldingClient
import no.nav.syfo.sykmelding.db.registrerEgenmeldtSykmelding
import no.nav.syfo.sykmelding.db.sykmeldingErAlleredeRegistrert
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForLangPeriodeException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForTidligsteFomException
import no.nav.syfo.sykmelding.errorhandling.exceptions.IkkeTilgangException
import no.nav.syfo.sykmelding.errorhandling.exceptions.OverlappMedEksisterendeSykmeldingException
import no.nav.syfo.sykmelding.errorhandling.exceptions.SykmeldingAlreadyExistsException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.mapping.opprettFellesformat
import no.nav.syfo.sykmelding.mapping.opprettReceivedSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Pasient
import no.nav.syfo.sykmelding.model.Periode
import no.nav.syfo.sykmelding.service.syfoservice.SyfoserviceService
import no.nav.syfo.sykmelding.util.extractHelseOpplysningerArbeidsuforhet

val tidligsteGyldigeFom: LocalDate = LocalDate.of(2020, 3, 1)
const val maxAntallDagerSykmeldt = 16

@KtorExperimentalAPI
class EgenmeldtSykmeldingService @KtorExperimentalAPI constructor(
    private val oppdaterTopicsService: OppdaterTopicsService,
    private val database: DatabaseInterface,
    private val pdlPersonService: PdlPersonService,
    private val syfoserviceService: SyfoserviceService,
    private val syfosmregisterSykmeldingClient: SyfosmregisterSykmeldingClient
) {

    suspend fun validerOgRegistrerEgenmeldtSykmelding(sykmeldingRequest: EgenmeldtSykmeldingRequest, fnr: String, session: Session, syfoserviceProducer: MessageProducer, userToken: String, callId: String) {
        val person = pdlPersonService.getPersonOgDiskresjonskode(fnr = fnr, userToken = userToken, callId = callId)
        val pasient = Pasient(
            fnr = fnr,
            aktorId = person.aktorId,
            fornavn = person.navn.fornavn,
            mellomnavn = person.navn.mellomnavn,
            etternavn = person.navn.etternavn)

        validerEgenmeldtSykmelding(
            fom = sykmeldingRequest.periode.fom,
            tom = sykmeldingRequest.periode.tom,
            harFortroligAdresse = person.fortroligAdresse,
            fnr = fnr,
            userToken = userToken,
            callId = callId
        )
        log.info("EgenmeldtSykmeldingRequest validert ok for callId {}", callId)

        val antallArbeidsgivere = sykmeldingRequest.arbeidsforhold.size
        if (sykmeldingRequest.arbeidsforhold.isEmpty()) {
            registrerEgenmeldtSykmelding(EgenmeldtSykmelding(UUID.randomUUID(), fnr, null, sykmeldingRequest.periode), session, syfoserviceProducer, pasient, antallArbeidsgivere, callId)
        } else {
            val list = sykmeldingRequest.arbeidsforhold.map {
                EgenmeldtSykmelding(UUID.randomUUID(), fnr, it, sykmeldingRequest.periode)
            }
            log.info("Oppretter {} sykmeldinger {}", list.size, callId)
            for (egenmeldtSykmelding in list) {
                registrerEgenmeldtSykmelding(egenmeldtSykmelding, session, syfoserviceProducer, pasient, antallArbeidsgivere, callId)
            }
        }
    }

    private fun registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmelding, session: Session, syfoserviceProducer: MessageProducer, pasient: Pasient, antallArbeidsgivere: Int, callId: String) {
        log.info("Registrerer sykmelding med id {} for callId {}", egenmeldtSykmelding.id, callId)
        database.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

        val fellesformat = opprettFellesformat(sykmeldt = pasient, sykmeldingId = egenmeldtSykmelding.id.toString(), fom = egenmeldtSykmelding.periode.fom, tom = egenmeldtSykmelding.periode.tom, arbeidsforhold = egenmeldtSykmelding.arbeidsforhold, antallArbeidsgivere = antallArbeidsgivere)
        val receivedSykmelding = opprettReceivedSykmelding(pasient = pasient, sykmeldingId = egenmeldtSykmelding.id.toString(), fellesformat = fellesformat)

        oppdaterTopicsService.oppdaterOKTopic(receivedSykmelding)
        syfoserviceService.sendTilSyfoservice(session, syfoserviceProducer, egenmeldtSykmelding.id.toString(), extractHelseOpplysningerArbeidsuforhet(fellesformat))
        EGENMELDT_SYKMELDING_COUNTER.inc()
    }

    suspend fun validerEgenmeldtSykmelding(fom: LocalDate, tom: LocalDate, harFortroligAdresse: Boolean, fnr: String, userToken: String, callId: String) {
        if (tom.isBefore(fom)) {
            log.warn("Tom-dato er før fom-dato for callId {}", callId)
            EGENMELDT_SYKMELDING_ERROR_TOM_BEFORE_FOM_COUNTER.inc()
            throw TomBeforeFomDateException("Tom-dato er før fom-dato")
        }
        if (harFortroligAdresse) {
            log.warn("Bruker har ikke tilgang til tjenesten, callId {}", callId)
            throw IkkeTilgangException("Bruker har ikke tilgang til tjenesten")
        }
        if (fom.isBefore(tidligsteGyldigeFom)) {
            log.warn("Egenmeldt sykmelding er ikke tilgjengelig før {}", tidligsteGyldigeFom)
            throw ForTidligsteFomException("Egenmeldt sykmelding er ikke tilgjengelig før")
        }
        if (tom.isAfter(fom.plusDays(maxAntallDagerSykmeldt.toLong()))) {
            log.warn("Egenmeldt sykmelding kan ikke være mer enn {} dager", maxAntallDagerSykmeldt)
            throw ForLangPeriodeException("Egenmeldt sykmelding kan ikke være lenger enn $maxAntallDagerSykmeldt dager")
        }
        if (database.sykmeldingErAlleredeRegistrert(fnr = fnr, periode = Periode(fom = fom, tom = tom))) {
            log.warn("Det finnes en sykmelding fra før for samme periode og bruker, {}", callId)
            EGENMELDT_SYKMELDING_ALREADY_EXISTS_COUNTER.inc()
            throw SykmeldingAlreadyExistsException("Det finnes en sykmelding fra før for samme periode og fødselsnummer")
        }
        if (harOverlappendeSykmeldingerIRegisteret(token = userToken, fom = fom, tom = tom)) {
            log.warn("Bruker har allerede sykmeldinger som overlapper med valgt periode {}", callId)
            throw OverlappMedEksisterendeSykmeldingException("Bruker har allerede sykmeldinger som overlapper med valgt periode")
        }
    }

    private suspend fun harOverlappendeSykmeldingerIRegisteret(token: String, fom: LocalDate, tom: LocalDate): Boolean {
        val tidligereSykmeldinger = syfosmregisterSykmeldingClient.getSykmeldinger(token = token, fom = fom, tom = tom)
        return tidligereSykmeldinger.isNotEmpty()
    }
}
