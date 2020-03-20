package no.nav.syfo.sykmelding.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import java.time.LocalDate
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Periode
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EgenmeldtSykmeldingServiceTest : Spek({
    val oppdaterTopicsService = mockk<OppdaterTopicsService>()
    val database = mockkClass(DatabaseInterface::class, relaxed = true)
    every { oppdaterTopicsService.oppdaterTopics(any()) } just Runs
    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, database)

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
