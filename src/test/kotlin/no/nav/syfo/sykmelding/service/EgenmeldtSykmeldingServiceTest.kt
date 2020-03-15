package no.nav.syfo.sykmelding.service

import java.time.LocalDate
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Periode
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EgenmeldtSykmeldingServiceTest : Spek({

    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService()

    describe("EgenmeldtSykmeldingService test") {
        it("Should be ok") {
            runBlocking {
                val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(Periode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(1)
                ))
                egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest)
            }
        }
        it("Should throw exception when tom is before form") {
            runBlocking {
                assertFailsWith<TomBeforeFomDateException>() {
                    val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(Periode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().minusDays(1)
                    ))
                    egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest)
                }
            }
        }
    }
})