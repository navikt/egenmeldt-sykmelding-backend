package no.nav.syfo.sykmelding.service

import io.ktor.util.KtorExperimentalAPI
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import java.time.LocalDate
import javax.jms.MessageProducer
import javax.jms.Session
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.pdl.model.Navn
import no.nav.syfo.pdl.model.PdlPerson
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.syfosmregister.client.SyfosmregisterSykmeldingClient
import no.nav.syfo.sykmelding.errorhandling.exceptions.IkkeTilgangException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Periode
import no.nav.syfo.sykmelding.service.syfoservice.SyfoserviceService
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalAPI
class EgenmeldtSykmeldingServiceTest : Spek({
    val usertoken = "token"
    val callId = "callId"
    val oppdaterTopicsService = mockk<OppdaterTopicsService>()
    val database = mockkClass(DatabaseInterface::class, relaxed = true)
    val session = mockk<Session>()
    val syfoserviceProducer = mockk<MessageProducer>()
    val syfoserviceService = mockk<SyfoserviceService>()
    val pdlService = mockk<PdlPersonService>()
    val syfosmregisterClient = mockk<SyfosmregisterSykmeldingClient>()
    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, database, pdlService, syfoserviceService, syfosmregisterClient)
    val person = PdlPerson(Navn(fornavn = "Fornavn", mellomnavn = "Mellomnavn", etternavn = "Etternavn"), false, "12345678910")

    beforeEachTest {
        clearAllMocks()
        every { oppdaterTopicsService.oppdaterOKTopic(any()) } just Runs
        every { syfoserviceService.sendTilSyfoservice(any(), any(), any(), any()) } just Runs
        coEvery { pdlService.getPersonOgDiskresjonskode(any(), any(), any()) } returns person
    }

    describe("EgenmeldtSykmeldingService test") {
        it("Should be ok") {
            runBlocking {
                val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1)),
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", session, syfoserviceProducer, usertoken, callId)
            }
        }
        it("Should throw exception when tom is before form") {
            runBlocking {
                assertFailsWith<TomBeforeFomDateException>() {
                    val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                            Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().minusDays(1)
                            ),
                            listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                    egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", session, syfoserviceProducer, usertoken, callId)
                }
            }
        }
        it("Bruker med fortrolig adresse skal ikke få tilgang") {
            coEvery { pdlService.getPersonOgDiskresjonskode(any(), any(), any()) } returns person.copy(fortroligAdresse = true)
            runBlocking {
                assertFailsWith<IkkeTilgangException>() {
                    val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().minusDays(1)
                        ),
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                    egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", session, syfoserviceProducer, usertoken, callId)
                }
            }
        }
    }
})
