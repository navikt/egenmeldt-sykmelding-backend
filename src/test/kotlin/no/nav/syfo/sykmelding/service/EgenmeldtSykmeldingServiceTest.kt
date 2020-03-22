package no.nav.syfo.sykmelding.service

import io.mockk.Runs
import io.mockk.clearAllMocks
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
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Periode
import no.nav.syfo.sykmelding.service.syfoservice.SyfoserviceService
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EgenmeldtSykmeldingServiceTest : Spek({
    val oppdaterTopicsService = mockk<OppdaterTopicsService>()
    val database = mockkClass(DatabaseInterface::class, relaxed = true)
    val session = mockk<Session>()
    val syfoserviceProducer = mockk<MessageProducer>()
    val syfoserviceService = mockk<SyfoserviceService>()
    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, database, syfoserviceService)

    beforeEachTest {
        clearAllMocks()
        every { oppdaterTopicsService.oppdaterOKTopic(any()) } just Runs
        every { syfoserviceService.sendTilSyfoservice(any(), any(), any(), any()) } just Runs
    }

    describe("EgenmeldtSykmeldingService test") {
        it("Should be ok") {
            runBlocking {
                val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1)),
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", session, syfoserviceProducer)
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
                    egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", session, syfoserviceProducer)
                }
            }
        }
    }
})
