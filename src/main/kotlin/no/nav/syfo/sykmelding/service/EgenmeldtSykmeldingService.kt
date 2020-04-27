package no.nav.syfo.sykmelding.service

import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDate
import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.log
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_COUNTER
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.syfosmregister.client.SyfosmregisterSykmeldingClient
import no.nav.syfo.sykmelding.db.antallSykmeldingerInnenforPeriode
import no.nav.syfo.sykmelding.db.registrerEgenmeldtSykmelding
import no.nav.syfo.sykmelding.db.sykmeldingOverlapperGrenseverdi
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForLangPeriodeException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForMangeEgenmeldingerException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForTidligsteFomException
import no.nav.syfo.sykmelding.errorhandling.exceptions.IkkeTilgangException
import no.nav.syfo.sykmelding.errorhandling.exceptions.OverlappMedEksisterendeSykmeldingException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.kafka.SykmeldingSyfoserviceKafkaProducer
import no.nav.syfo.sykmelding.mapping.opprettFellesformat
import no.nav.syfo.sykmelding.mapping.opprettReceivedSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Pasient
import no.nav.syfo.sykmelding.util.extractHelseOpplysningerArbeidsuforhet

const val maxAntallDagerSykmeldt = 16

@KtorExperimentalAPI
class EgenmeldtSykmeldingService @KtorExperimentalAPI constructor(
    private val oppdaterTopicsService: OppdaterTopicsService,
    private val database: DatabaseInterface,
    private val pdlPersonService: PdlPersonService,
    private val syfoserviceServiceKafkaProducer: SykmeldingSyfoserviceKafkaProducer,
    private val syfosmregisterSykmeldingClient: SyfosmregisterSykmeldingClient
) {

    suspend fun validerOgRegistrerEgenmeldtSykmelding(sykmeldingRequest: EgenmeldtSykmeldingRequest, fnr: String, userToken: String, callId: String) {
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
            registrerEgenmeldtSykmelding(EgenmeldtSykmelding(UUID.randomUUID(), fnr, null, sykmeldingRequest.periode, sykmeldingRequest.egenSykdom), pasient, antallArbeidsgivere, callId)
        } else {
            val list = sykmeldingRequest.arbeidsforhold.map {
                EgenmeldtSykmelding(UUID.randomUUID(), fnr, it, sykmeldingRequest.periode, sykmeldingRequest.egenSykdom)
            }
            log.info("Oppretter {} sykmeldinger {}", list.size, callId)
            for (egenmeldtSykmelding in list) {
                registrerEgenmeldtSykmelding(egenmeldtSykmelding, pasient, antallArbeidsgivere, callId)
            }
        }
    }

    private fun registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmelding, pasient: Pasient, antallArbeidsgivere: Int, callId: String) {
        log.info("Registrerer sykmelding med id {} for callId {}", egenmeldtSykmelding.id, callId)
        database.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

        val fellesformat = opprettFellesformat(sykmeldt = pasient, sykmeldingId = egenmeldtSykmelding.id.toString(), fom = egenmeldtSykmelding.periode.fom, tom = egenmeldtSykmelding.periode.tom, arbeidsforhold = egenmeldtSykmelding.arbeidsforhold, antallArbeidsgivere = antallArbeidsgivere)
        val receivedSykmelding = opprettReceivedSykmelding(pasient = pasient, sykmeldingId = egenmeldtSykmelding.id.toString(), fellesformat = fellesformat)

        oppdaterTopicsService.oppdaterOKTopic(receivedSykmelding)
        syfoserviceServiceKafkaProducer.publishSykmeldingToKafka(egenmeldtSykmelding.id.toString(), extractHelseOpplysningerArbeidsuforhet(fellesformat))
        EGENMELDT_SYKMELDING_COUNTER.inc()
    }

    suspend fun validerEgenmeldtSykmelding(fom: LocalDate, tom: LocalDate, harFortroligAdresse: Boolean, fnr: String, userToken: String, callId: String) {
        if (tom.isBefore(fom)) {
            log.warn("Tom-dato er før fom-dato for callId {}", callId)
            throw TomBeforeFomDateException("Tom-dato er før fom-dato")
        }
        if (harFortroligAdresse) {
            log.warn("Bruker har ikke tilgang til tjenesten, callId {}", callId)
            throw IkkeTilgangException("Du har dessverre ikke tilgang til tjenesten")
        }
        if (fom.isBefore(LocalDate.now())) {
            log.warn("Egenmelding kan ikke starte før dagens dato, fom: {} dagens dato {}", fom, LocalDate.now())
            throw ForTidligsteFomException("Egenmelding kan ikke starte før dagens dato")
        }
        if (tom.isAfter(fom.plusDays(maxAntallDagerSykmeldt.toLong()))) {
            log.warn("Egenmeldt sykmelding kan ikke være mer enn {} dager", maxAntallDagerSykmeldt)
            throw ForLangPeriodeException("Egenmeldt sykmelding kan ikke være lenger enn $maxAntallDagerSykmeldt dager")
        }
        if (database.sykmeldingOverlapperGrenseverdi(fnr, fom, tom, 16) ||
                database.antallSykmeldingerInnenforPeriode(fnr,
                        fom = LocalDate.now().minusMonths(4), tom =  LocalDate.now()) > 1) {
            log.warn("Man kan bare ha opp til to egenmeldt sykmeldinger ila. 4 måneder, med minimum 16 dagers opphold")
            throw ForMangeEgenmeldingerException("Du kan bare ha opp til to egenmeldinger i løpet av fire måneder, med minimum 16 dagers opphold")
        }
        if (harOverlappendeSykmeldingerIRegisteret(token = userToken, fom = fom, tom = tom)) {
            log.warn("Bruker har allerede sykmeldinger som overlapper med valgt periode {}", callId)
            throw OverlappMedEksisterendeSykmeldingException("Du har allerede levert sykmelding, da skal du ikke bruke denne egenmeldingen")
        }
    }

    private suspend fun harOverlappendeSykmeldingerIRegisteret(token: String, fom: LocalDate, tom: LocalDate): Boolean {
        val tidligereSykmeldinger = syfosmregisterSykmeldingClient.getSykmeldinger(token = token, fom = fom, tom = tom)
        return tidligereSykmeldinger.isNotEmpty()
    }
}
