package no.nav.syfo.sykmelding.api

import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.util.KtorExperimentalAPI
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import java.time.LocalDate
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.pdl.model.Navn
import no.nav.syfo.pdl.model.PdlPerson
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.syfosmregister.client.SyfosmregisterSykmeldingClient
import no.nav.syfo.sykmelding.kafka.SykmeldingSyfoserviceKafkaProducer
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

@KtorExperimentalAPI
class EgenmeldtSykmeldingApiKtTest : Spek({
    val oppdaterTopicsService = mockk<OppdaterTopicsService>()
    val database = mockkClass(DatabaseInterface::class, relaxed = true)

    val syfoserviceKafkaProducer = mockk<SykmeldingSyfoserviceKafkaProducer>()
    val pdlService = mockk<PdlPersonService>()
    val syfosmregisterClient = mockk<SyfosmregisterSykmeldingClient>()
    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, database, pdlService, syfoserviceKafkaProducer, syfosmregisterClient)
    val person = PdlPerson(Navn(fornavn = "Fornavn", mellomnavn = "Mellomnavn", etternavn = "Etternavn"), false, "12345678910")

    beforeEachTest {
        clearAllMocks()
        every { oppdaterTopicsService.oppdaterOKTopic(any()) } just Runs
        every { syfoserviceKafkaProducer.publishSykmeldingToKafka(any(), any()) } just Runs
        coEvery { pdlService.getPersonOgDiskresjonskode(any(), any(), any()) } returns person
        coEvery { syfosmregisterClient.getSykmeldinger(any(), any(), any()) } returns emptyList()
        ikkeTilgjengeligFra = LocalDate.now().plusDays(20)
    }

    describe("Test EgenmeldtSykmeldingApi") {
        with(TestApplicationEngine()) {
            start()
            setUpTestApplication()
            setUpAuth()
            val applicationState = ApplicationState(alive = true, ready = true)

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
                            arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)),
                            egenSykdom = false
                    )
                    setBody(getObjectMapper().writeValueAsString(egenmeldtSykmelding))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                            "loginservice",
                            subject = "12345678910",
                            issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.Created
                }
            }

            it("Skal overstyre TOM") {
                with(handleRequest(HttpMethod.Post, "api/v1/sykmelding/egenmeldt") {
                    val egenmeldtSykmelding = EgenmeldtSykmeldingRequest(
                            periode = Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().minusDays(4)),
                            arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)),
                            egenSykdom = true
                    )
                    setBody(getObjectMapper().writeValueAsString(egenmeldtSykmelding))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                            "loginservice",
                            subject = "12345678910",
                            issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.Created
                }
            }
        }
    }

    describe("Test Api with authentication") {
        with(TestApplicationEngine()) {
            start()
            setUpTestApplication()
            setUpAuth()

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
                            arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)),
                            egenSykdom = true
                    )
                    setBody(getObjectMapper().writeValueAsString(requestBody))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                            "loginservice",
                            subject = "12345678910",
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
                            true,
                            listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5))

                    )
                    setBody(getObjectMapper().writeValueAsString(requestBody))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                            "loginservice2",
                            subject = "12345678910",
                            issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.Unauthorized
                }
            }
        }
    }

    describe("EgenmeldtSykmeldingApi skal ikke være tilgjengelig etter gitt dato") {
        with(TestApplicationEngine()) {
            start()
            setUpTestApplication()
            setUpAuth()
            val applicationState = ApplicationState(alive = true, ready = true)

            application.routing {
                registerNaisApi(applicationState)
                authenticate {
                    registrerEgenmeldtSykmeldingApi(egenmeldtSykmeldingService)
                }
            }
            it("API skal være tilgjengelig") {
                ikkeTilgjengeligFra = LocalDate.now().plusDays(1)

                with(handleRequest(HttpMethod.Post, "api/v1/sykmelding/egenmeldt") {
                    val egenmeldtSykmelding = EgenmeldtSykmeldingRequest(
                        periode = Periode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(4)),
                        arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)),
                        egenSykdom = false
                    )
                    setBody(getObjectMapper().writeValueAsString(egenmeldtSykmelding))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                        "loginservice",
                        subject = "12345678910",
                        issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.Created
                }
            }
            it("API skal ikke være tilgjengelig samme dag") {
                ikkeTilgjengeligFra = LocalDate.now()

                with(handleRequest(HttpMethod.Post, "api/v1/sykmelding/egenmeldt") {
                    val egenmeldtSykmelding = EgenmeldtSykmeldingRequest(
                        periode = Periode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(4)),
                        arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)),
                        egenSykdom = false
                    )
                    setBody(getObjectMapper().writeValueAsString(egenmeldtSykmelding))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                        "loginservice",
                        subject = "12345678910",
                        issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.ServiceUnavailable
                }
            }
            it("API skal ikke være tilgjengelig") {
                ikkeTilgjengeligFra = LocalDate.now().minusDays(1)

                with(handleRequest(HttpMethod.Post, "api/v1/sykmelding/egenmeldt") {
                    val egenmeldtSykmelding = EgenmeldtSykmeldingRequest(
                        periode = Periode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(4)),
                        arbeidsforhold = listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)),
                        egenSykdom = false
                    )
                    setBody(getObjectMapper().writeValueAsString(egenmeldtSykmelding))
                    addHeader("Content-Type", "application/json")
                    addHeader("Authorization", "Bearer ${generateJWT("client",
                        "loginservice",
                        subject = "12345678910",
                        issuer = "issuer")}")
                }) {
                    response.status() shouldEqual HttpStatusCode.ServiceUnavailable
                }
            }
        }
    }
})
