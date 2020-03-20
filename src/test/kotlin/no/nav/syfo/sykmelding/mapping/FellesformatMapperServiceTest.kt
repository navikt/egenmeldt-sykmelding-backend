package no.nav.syfo.sykmelding.mapping

import java.io.StringReader
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.syfo.sykmelding.model.Pasient
import no.nav.syfo.sykmelding.util.fellesformatMarshaller
import no.nav.syfo.sykmelding.util.fellesformatUnmarshaller
import no.nav.syfo.sykmelding.util.toString
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class FellesformatMapperServiceTest : Spek({
    val sykmeldingId = "123456"
    val pasient = Pasient("12345678910", "1111", "Fornavn", null, "Etternavn")
    describe("Test av mapping") {
        it("Skal kunne unmarshalle xml fra mapperen") {
            val fellesformat = opprettFellesformat(pasient, sykmeldingId)

            fellesformatUnmarshaller.unmarshal(StringReader(fellesformatMarshaller.toString(fellesformat))) as XMLEIFellesformat
        }
    }
})
