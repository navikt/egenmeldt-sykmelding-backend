package no.nav.syfo.sykmelding.api

import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.clearAllMocks
import io.mockk.mockkClass
import java.time.LocalDate
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.sykmelding.errorhandling.EgenmeldtSykmeldingError
import no.nav.syfo.sykmelding.errorhandling.ErrorResponse
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Periode
import no.nav.syfo.sykmelding.service.EgenmeldtSykmeldingService
import no.nav.syfo.sykmelding.service.OppdaterTopicsService
import no.nav.syfo.sykmelding.util.generateJWT
import no.nav.syfo.sykmelding.util.getObjectMapper
import no.nav.syfo.sykmelding.util.setUpAuth
import no.nav.syfo.sykmelding.util.setUpTestApplication
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EgenmeldtSykmeldingApiKtTest : Spek({
    val oppdaterTopicsService = mockk<OppdaterTopicsService>()
    every { oppdaterTopicsService.oppdaterTopics(any()) } just Runs
    describe("Test EgenmeldtSykmeldingApi") {
        with(TestApplicationEngine()) {
            start()
            setUpTestApplication()
            setUpAuth()
            val database = mockkClass(DatabaseInterface::class, relaxed = true)
            val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, database)
            val applicationState = ApplicationState(alive = true, ready = true)

            beforeEachTest {
                clearAllMocks()
            }

            application.routing {
                registerNaisApi(applicationState)
                authenticate {
                    registrerEgenmeldtSykmeldingApi(egenmeldtSykmeldingService)
                }
            }

            it("Should be OK") {
                with(handleRequest(HttpMethod.Post, "api/v1/sykmelding/egenmeldt") {
                    val egenmeldtSykmelding = EgenmeldtSykmeldingRequest(
                            periode = Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(4)),
                            arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5))
                    )
                    setBody(getObjectMapper().writeValueAsString(egenmeldtSykmelding))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                        "loginservice",
                        subject = "12345678901",
                        issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.Created
                }
            }

            it("Should get bad request") {
                with(handleRequest(HttpMethod.Post, "api/v1/sykmelding/egenmeldt") {
                    val egenmeldtSykmelding = EgenmeldtSykmeldingRequest(
                            periode = Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().minusDays(4)),
                            arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5))
                    )
                    setBody(getObjectMapper().writeValueAsString(egenmeldtSykmelding))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                        "loginservice",
                        subject = "12345678901",
                        issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.BadRequest
                    getObjectMapper().readValue(response.content, ErrorResponse::class.java) shouldEqual ErrorResponse(listOf(EgenmeldtSykmeldingError("Tom date is before Fom date")))
                }
            }
        }
    }

    describe("Test Api with authentication") {
        with(TestApplicationEngine()) {
            start()
            setUpTestApplication()
            setUpAuth()
            val database = mockkClass(DatabaseInterface::class, relaxed = true)
            val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, database)
            application.routing {
                authenticate {
                    registrerEgenmeldtSykmeldingApi(egenmeldtSykmeldingService)
                }
            }

            it("Should authenticate OK") {
                with(handleRequest(HttpMethod.Post, "api/v1/sykmelding/egenmeldt") {
                    val requestBody = EgenmeldtSykmeldingRequest(Periode(
                            LocalDate.now(),
                            LocalDate.now()),
                            arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5))
                    )
                    setBody(getObjectMapper().writeValueAsString(requestBody))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                            "loginservice",
                            subject = "12345678901",
                            issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.Created
                }
            }
            it("Should get Unauthorized") {
                with(handleRequest(HttpMethod.Post, "api/v1/sykmelding/egenmeldt") {
                    val requestBody = EgenmeldtSykmeldingRequest(Periode(
                            LocalDate.now(),
                            LocalDate.now()),
                            listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5))
                    )
                    setBody(getObjectMapper().writeValueAsString(requestBody))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                            "loginservice2",
                            subject = "12345678901",
                            issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.Unauthorized
                }
            }
        }
    }
})
