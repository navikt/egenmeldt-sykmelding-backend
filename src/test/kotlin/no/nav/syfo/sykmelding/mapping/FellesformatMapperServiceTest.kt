package no.nav.syfo.sykmelding.mapping

import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter
import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.sykmelding.model.Pasient
import no.nav.syfo.sykmelding.util.XMLDateAdapter
import no.nav.syfo.sykmelding.util.XMLDateTimeAdapter
import no.nav.syfo.sykmelding.util.fellesformatJaxBContext
import no.nav.syfo.sykmelding.util.toString
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class FellesformatMapperServiceTest : Spek({
    val fellesformatUnmarshaller: Unmarshaller = fellesformatJaxBContext.createUnmarshaller().apply {
        setAdapter(LocalDateTimeXmlAdapter::class.java, XMLDateTimeAdapter())
        setAdapter(LocalDateXmlAdapter::class.java, XMLDateAdapter())
    }
    val fellesformatMarshaller: Marshaller = JAXBContext.newInstance(XMLEIFellesformat::class.java, XMLMsgHead::class.java, HelseOpplysningerArbeidsuforhet::class.java).createMarshaller()
        .apply { setProperty(Marshaller.JAXB_ENCODING, "UTF-8") }

    val sykmeldingId = "123456"
    val pasient = Pasient("12345678910", "1111", "Fornavn", null, "Etternavn")
    describe("Test av mapping") {
        it("Skal kunne unmarshalle xml fra mapperen") {
            val fellesformat = opprettFellesformat(pasient, sykmeldingId)

            fellesformatUnmarshaller.unmarshal(StringReader(fellesformatMarshaller.toString(fellesformat))) as XMLEIFellesformat
        }
    }
})
