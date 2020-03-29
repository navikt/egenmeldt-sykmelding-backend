package no.nav.syfo.status.service

import io.mockk.clearAllMocks
import io.mockk.mockkClass
import io.mockk.verify
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.StatusEventDTO
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.syfo.sykmelding.db.finnEgenmeldtSykmelding
import no.nav.syfo.sykmelding.db.registrerEgenmeldtSykmelding
import no.nav.syfo.sykmelding.db.slettEgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.Periode
import no.nav.syfo.sykmelding.util.TestDB
import no.nav.syfo.sykmelding.util.dropData
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class StatusendringServiceTest : Spek({
    val sykmeldingId = UUID.randomUUID()
    val databaseMock = mockkClass(DatabaseInterface::class, relaxed = true)

    val testDB = TestDB()

    afterEachTest {
        clearAllMocks()
        testDB.connection.dropData()
    }

    afterGroup {
        testDB.stop()
    }

    describe("Test av håndtering av statusendringer") {
        val statusendringServiceMedMockDB = StatusendringService(databaseMock)
        it("Skal ignorere statusevent APEN") {
            statusendringServiceMedMockDB.handterStatusendring(opprettKafkaMelding(sykmeldingId.toString(), "fnr", StatusEventDTO.APEN))

            verify(exactly = 0) { databaseMock.slettEgenmeldtSykmelding(any()) }
        }
        it("Skal kalle finnOgSlett for statusevent AVBRUTT") {
            statusendringServiceMedMockDB.handterStatusendring(opprettKafkaMelding(sykmeldingId.toString(), "fnr", StatusEventDTO.AVBRUTT))

            verify(exactly = 1) { databaseMock.slettEgenmeldtSykmelding(any()) }
        }
    }

    describe("Test av håndtering av statusendringer mot database") {
        val statusendringService = StatusendringService(testDB)
        it("Skal slette sykmelding hvis den finnes i databasen") {
            testDB.registrerEgenmeldtSykmelding(EgenmeldtSykmelding(sykmeldingId, "fnr", null, Periode(LocalDate.now().minusDays(5), LocalDate.now().plusDays(11))))
            testDB.finnEgenmeldtSykmelding(sykmeldingId) shouldNotBe null

            statusendringService.handterStatusendring(opprettKafkaMelding(sykmeldingId.toString(), "fnr", StatusEventDTO.AVBRUTT))

            testDB.finnEgenmeldtSykmelding(sykmeldingId) shouldEqual null
        }
        it("Skal ikke feile hvis det ikke er noen sykmelding å slette for statusevent AVBRUTT") {
            statusendringService.handterStatusendring(opprettKafkaMelding(sykmeldingId.toString(), "annetfnr", StatusEventDTO.AVBRUTT))
        }
    }
})

fun opprettKafkaMelding(sykmeldingId: String, fnr: String, status: StatusEventDTO): SykmeldingStatusKafkaMessageDTO =
    SykmeldingStatusKafkaMessageDTO(
        KafkaMetadataDTO(sykmeldingId = sykmeldingId, timestamp = OffsetDateTime.now(), fnr = fnr, source = "syfoservice"),
        SykmeldingStatusKafkaEventDTO(sykmeldingId = sykmeldingId,
            timestamp = OffsetDateTime.now(),
            statusEvent = status,
            arbeidsgiver = null,
            sporsmals = emptyList()
        )
    )
