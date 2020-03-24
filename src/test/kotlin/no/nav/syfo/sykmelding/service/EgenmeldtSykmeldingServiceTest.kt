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
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.syfosmregister.client.SyfosmregisterSykmeldingClient
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.integration.aktor.client.AktoerIdClient
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Periode
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalAPI
class EgenmeldtSykmeldingServiceTest : Spek({
    val oppdaterTopicsService = mockk<OppdaterTopicsService>()
    val aktoerIdClient = mockk<AktoerIdClient>()
    val database = mockkClass(DatabaseInterface::class, relaxed = true)
    val pdlService = mockk<PdlPersonService>()
    val syfosmregisterClient = mockk<SyfosmregisterSykmeldingClient>()
    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, aktoerIdClient, database, pdlService, syfosmregisterClient)

    beforeEachTest {
        clearAllMocks()
        every { oppdaterTopicsService.oppdaterOKTopic(any()) } just Runs
        coEvery { aktoerIdClient.finnAktoerId(any(), any()) } returns "12345678910"
    }

    describe("EgenmeldtSykmeldingService test") {
        it("Should be ok") {
            runBlocking {
                val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1)),
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910")
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
                    egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910")
                }
            }
        }
    }
})
