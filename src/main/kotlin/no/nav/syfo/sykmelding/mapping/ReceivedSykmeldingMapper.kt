package no.nav.syfo.sykmelding.mapping
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.xml.bind.Marshaller
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.sykmelding.model.Pasient
import no.nav.syfo.sykmelding.util.extractHelseOpplysningerArbeidsuforhet
import no.nav.syfo.sykmelding.util.fellesformatJaxBContext
import no.nav.syfo.sykmelding.util.get
import no.nav.syfo.sykmelding.util.toString

const val dummyTssIdent = "80000821845"

fun opprettReceivedSykmelding(pasient: Pasient, sykmeldingId: String, fellesformat: XMLEIFellesformat): ReceivedSykmelding {
    val fellesformatMarshaller: Marshaller = fellesformatJaxBContext.createMarshaller()
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
        mottattDato = OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime(),
        rulesetVersion = healthInformation.regelSettVersjon,
        fellesformat = fellesformatMarshaller.toString(fellesformat),
        tssid = dummyTssIdent
    )
}
