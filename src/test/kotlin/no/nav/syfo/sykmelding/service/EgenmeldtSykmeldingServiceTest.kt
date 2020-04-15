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
import no.nav.syfo.pdl.model.Navn
import no.nav.syfo.pdl.model.PdlPerson
import no.nav.syfo.pdl.service.PdlPersonService
import no.nav.syfo.syfosmregister.client.SyfosmregisterSykmeldingClient
import no.nav.syfo.syfosmregister.model.BehandlingsutfallDTO
import no.nav.syfo.syfosmregister.model.BehandlingsutfallStatusDTO
import no.nav.syfo.syfosmregister.model.SykmeldingDTO
import no.nav.syfo.syfosmregister.model.SykmeldingsperiodeDTO
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForLangPeriodeException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForTidligsteFomException
import no.nav.syfo.sykmelding.errorhandling.exceptions.IkkeTilgangException
import no.nav.syfo.sykmelding.errorhandling.exceptions.OverlappMedEksisterendeSykmeldingException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException
import no.nav.syfo.sykmelding.kafka.SykmeldingSyfoserviceKafkaProducer
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.model.Periode
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalAPI
class EgenmeldtSykmeldingServiceTest : Spek({
    val usertoken = "token"
    val callId = "callId"
    val oppdaterTopicsService = mockk<OppdaterTopicsService>()
    val database = mockkClass(DatabaseInterface::class, relaxed = true)
    val syfoserviceProducer = mockk<SykmeldingSyfoserviceKafkaProducer>()
    val pdlService = mockk<PdlPersonService>()
    val syfosmregisterClient = mockk<SyfosmregisterSykmeldingClient>()
    val egenmeldtSykmeldingService = EgenmeldtSykmeldingService(oppdaterTopicsService, database, pdlService, syfoserviceProducer, syfosmregisterClient)
    val person = PdlPerson(Navn(fornavn = "Fornavn", mellomnavn = "Mellomnavn", etternavn = "Etternavn"), false, "12345678910")

    beforeEachTest {
        clearAllMocks()
        every { oppdaterTopicsService.oppdaterOKTopic(any()) } just Runs
        every { syfoserviceProducer.publishSykmeldingToKafka(any(), any()) } just Runs
        coEvery { pdlService.getPersonOgDiskresjonskode(any(), any(), any()) } returns person
        coEvery { syfosmregisterClient.getSykmeldinger(any(), any(), any()) } returns emptyList()
    }

    describe("Test av valideringsregler") {
        it("Should be ok") {
            runBlocking {
                val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                                fom = LocalDate.now(),
                                tom = LocalDate.now().plusDays(1)),
                        true,
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", usertoken, callId)
            }
        }
        it("Skal feile hvis FOM er før egenmeldt sykmelding er tilgjengelig") {
            runBlocking {
                assertFailsWith<ForTidligsteFomException>() {
                    val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                            fom = LocalDate.now().minusDays(1),
                            tom = LocalDate.now().plusDays(10)),
                            true,
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                    egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", usertoken, callId)
                }
            }
        }
        it("Skal gå ok hvis FOM er samme dag som egenmeldt sykmelding ble tilgjengelig") {
            runBlocking {
                val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                    Periode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(7)),
                        true,
                    listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", usertoken, callId)
            }
        }
        it("Skal feile hvis perioden for egenmeldt sykmelding er for lang") {
            runBlocking {
                assertFailsWith<ForLangPeriodeException>() {
                    val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now().plusDays(maxAntallDagerSykmeldt.toLong() + 1)),
                            true,
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                    egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", usertoken, callId)
                }
            }
        }
        it("Skal gå ok hvis perioden for egenmeldt sykmelding er akkurat 16 dager") {
            runBlocking {
                val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                    Periode(
                        fom = LocalDate.now(),
                        tom = LocalDate.now().plusDays(maxAntallDagerSykmeldt.toLong())),
                        true,
                    listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", usertoken, callId)
            }
        }
        it("Should throw exception when tom is before fom") {
            runBlocking {
                assertFailsWith<TomBeforeFomDateException>() {
                    val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                            Periode(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().minusDays(1)
                            ),
                            true,
                            listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                    egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", usertoken, callId)
                }
            }
        }
        it("Bruker med fortrolig adresse skal ikke få tilgang") {
            coEvery { pdlService.getPersonOgDiskresjonskode(any(), any(), any()) } returns person.copy(fortroligAdresse = true)
            runBlocking {
                assertFailsWith<IkkeTilgangException>() {
                    val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                            fom = LocalDate.now().minusDays(1),
                            tom = LocalDate.now()
                        ),
                            true,
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))
                    egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", usertoken, callId)
                }
            }
        }
        it("Får feilmelding hvis bruker har overlappende sykmeldinger i registeret fra før") {
            coEvery { syfosmregisterClient.getSykmeldinger(any(), any(), any()) } returns listOf(SykmeldingDTO(
                id = "123456",
                sykmeldingsperioder = listOf(SykmeldingsperiodeDTO(LocalDate.now().minusDays(10), LocalDate.now().plusDays(5), null)),
                medisinskVurdering = null,
                behandlingsutfall = BehandlingsutfallDTO(emptyList(), BehandlingsutfallStatusDTO.OK)))
            runBlocking {
                assertFailsWith<OverlappMedEksisterendeSykmeldingException>() {
                    val egenmeldtSykmeldingRequest = EgenmeldtSykmeldingRequest(
                        Periode(
                            fom = LocalDate.now(),
                            tom = LocalDate.now()
                        ),
                            true,
                        listOf(Arbeidsforhold("arbeidsgiver", "123456789", 50.5)))

                    egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, "12345678910", usertoken, callId)
                }
            }
        }
    }
})
