package no.nav.syfo.sykmelding.service

import java.time.LocalDate
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Periode
import no.nav.syfo.sykmelding.util.TestDB
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EgenmeldtSykmeldingServiceTest : Spek({


    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(TestDB())

    describe("EgenmeldtSykmeldingService test") {
        it("Should be ok") {
            runBlocking {
                val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1)),
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))

                egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678912")
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
                    egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678912")
                }
            }
        }
    }
})
