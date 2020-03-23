package no.nav.syfo.sykmelding.service

import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDateTime
import java.util.UUID
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.log
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_COUNTER
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_FAILED_COUNTER
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_PERMUTED_COUNTER
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.sykmelding.db.registrerEgenmeldtSykmelding
import no.nav.syfo.sykmelding.db.sykmeldingOverlapper
import no.nav.syfo.sykmelding.errorhandling.exceptions.SykmeldingAlreadyExistsException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.integration.aktor.client.AktoerIdClient
import no.nav.syfo.sykmelding.mapping.opprettFellesformat
import no.nav.syfo.sykmelding.mapping.toSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Pasient
import no.nav.syfo.sykmelding.service.syfoservice.SyfoserviceService
import no.nav.syfo.sykmelding.util.extractHelseOpplysningerArbeidsuforhet
import no.nav.syfo.sykmelding.util.get
import no.nav.syfo.sykmelding.util.toString

@KtorExperimentalAPI
class EgenmeldtSykmeldingService @KtorExperimentalAPI constructor(
    private val oppdaterTopicsService: OppdaterTopicsService,
    private val aktoerIdClient: AktoerIdClient,
    private val database: DatabaseInterface,
    private val pdlPersonService: PdlPersonService,
    private val syfoserviceService: SyfoserviceService
) {

    private val dummyTssIdent = "80000821845"

    suspend fun registrerEgenmeldtSykmelding(sykmeldingRequest: EgenmeldtSykmeldingRequest, fnr: String, session: Session, syfoserviceProducer: MessageProducer) {
        if (sykmeldingRequest.arbeidsforhold.isEmpty()) {
            log.info("Registrerer sykmelding uten arbeidsforhold")
            registrerEgenmeldtSykmelding(EgenmeldtSykmelding(UUID.randomUUID(), fnr, null, sykmeldingRequest.periode), session, syfoserviceProducer)
        } else {
            val list = sykmeldingRequest.arbeidsforhold.map {
                EgenmeldtSykmelding(UUID.randomUUID(), fnr, it, sykmeldingRequest.periode)
            }
            log.info("Oppretter {} sykmeldinger", list.size)
            for (egenmeldtSykmelding in list) {
                registrerEgenmeldtSykmelding(egenmeldtSykmelding, session, syfoserviceProducer)
            }
        }

        EGENMELDT_SYKMELDING_COUNTER.inc()
    }

    private suspend fun registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmelding, session: Session, syfoserviceProducer: MessageProducer) {
        log.info("Mottatt sykmelding med id {}", egenmeldtSykmelding.id)
        val fom = egenmeldtSykmelding.periode.fom
        val tom = egenmeldtSykmelding.periode.tom
        if (tom.isBefore(fom)) {
            EGENMELDT_SYKMELDING_FAILED_COUNTER.inc()
            log.warn("Tom-dato er før fom-dato for sykmeldingid {}", egenmeldtSykmelding.id)
            throw TomBeforeFomDateException("Tom date is before Fom date")
        }

        if (database.sykmeldingOverlapper(egenmeldtSykmelding)) {
            EGENMELDT_SYKMELDING_FAILED_COUNTER.inc()
            log.error("Det finnes en sykmelding fra før for samme arbeidsgiver og samme bruker, {}", egenmeldtSykmelding.id)
            throw SykmeldingAlreadyExistsException("A sykmelding with the same arbeidsgiver already exists for the given fødselsnummer")
        }
        database.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

        val fnr = egenmeldtSykmelding.fnr
        val sykmeldingId = egenmeldtSykmelding.id.toString()

        val pasientIdent = aktoerIdClient.finnAktoerId(fnr, sykmeldingId)!!

        val pasient = Pasient(
            fnr = fnr,
            aktorId = pasientIdent,
            fornavn = "Fanny",
            mellomnavn = null,
            etternavn = "Storm")
        val fellesformat = opprettFellesformat(sykmeldt = pasient, sykmeldingId = egenmeldtSykmelding.id.toString())
        val receivedSykmelding = opprettReceivedSykmelding(pasient = pasient, sykmeldingId = egenmeldtSykmelding.id.toString(), fellesformat = fellesformat)

        EGENMELDT_SYKMELDING_PERMUTED_COUNTER.inc()

        oppdaterTopicsService.oppdaterOKTopic(receivedSykmelding)
        syfoserviceService.sendTilSyfoservice(session, syfoserviceProducer, egenmeldtSykmelding.id.toString(), extractHelseOpplysningerArbeidsuforhet(fellesformat))
    }

    private fun opprettReceivedSykmelding(pasient: Pasient, sykmeldingId: String, fellesformat: XMLEIFellesformat): ReceivedSykmelding {
        val fellesformatMarshaller: Marshaller = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLMsgHead::class.java, HelseOpplysningerArbeidsuforhet::class.java).createMarshaller()
            .apply { setProperty(Marshaller.JAXB_ENCODING, "UTF-8") }

        val healthInformation = extractHelseOpplysningerArbeidsuforhet(fellesformat)
        val msgHead = fellesformat.get<XMLMsgHead>()

        val sykmelding = healthInformation.toSykmelding(
            sykmeldingId = sykmeldingId,
            pasientAktoerId = pasient.aktorId,
            legeAktoerId = pasient.aktorId,
            msgId = sykmeldingId,
            signaturDato = msgHead.msgInfo.genDate
        )
        return ReceivedSykmelding(
            sykmelding = sykmelding,
            personNrPasient = pasient.fnr,
            tlfPasient = healthInformation.pasient.kontaktInfo.firstOrNull()?.teleAddress?.v,
            personNrLege = pasient.fnr,
            navLogId = sykmeldingId,
            msgId = sykmeldingId,
            legekontorOrgNr = "889640782",
            legekontorOrgName = "NAV",
            legekontorHerId = null,
            legekontorReshId = null,
            mottattDato = LocalDateTime.now(),
            rulesetVersion = healthInformation.regelSettVersjon,
            fellesformat = fellesformatMarshaller.toString(fellesformat),
            tssid = dummyTssIdent
        )
    }
}
