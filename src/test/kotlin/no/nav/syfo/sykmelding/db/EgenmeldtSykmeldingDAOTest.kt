package no.nav.syfo.sykmelding.db

import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.Periode
import no.nav.syfo.sykmelding.util.TestDB
import no.nav.syfo.sykmelding.util.dropData
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EgenmeldtSykmeldingDAOTest : Spek({

    val testDB = TestDB()

    afterEachTest {
        testDB.connection.dropData()
    }

    afterGroup {
        testDB.stop()
    }

    describe("EgenmeldtSykmeldingDAO test") {
        val egenmeldtSykmelding = EgenmeldtSykmelding(UUID.randomUUID(),
                "12345678912",
                Arbeidsforhold("arbeidsgiver", "123456789", 50.5),
                Periode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1)))

        it("Should not overlap when not exists") {
            assertFalse(testDB.sykmeldingOverlapper(egenmeldtSykmelding))
        }

        it("Should insert") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)
        }

        it("Should overlap after insert") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

            assertTrue(testDB.sykmeldingOverlapper(egenmeldtSykmelding))
        }

        it("Should allow arbeidsforhold = null") {
            val egenmeldtSykmeldingUtenArbeidsforhold = EgenmeldtSykmelding(UUID.randomUUID(),
                "12345678912",
                null,
                Periode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1)))

            assertFalse(testDB.sykmeldingOverlapper(egenmeldtSykmeldingUtenArbeidsforhold))
        }

        it("Should  query by id") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

            val res = testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.id)

            assertEquals(egenmeldtSykmelding, res)
        }

        it("Should  query by fødselsnummer") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

            val res = testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.fnr)

            assertEquals(egenmeldtSykmelding, res)
        }
    }

    describe("Verifisering av duplikatsjekk") {
        it("sykmeldingErAlleredeRegistrert skal returnere false når sykmelding ikke er registrert") {
            testDB.sykmeldingErAlleredeRegistrertForBruker("fnr") shouldEqual false
        }

        it("sykmeldingErAlleredeRegistrert skal returnere true når en lik sykmelding er registrert fra før") {
            testDB.registrerEgenmeldtSykmelding(EgenmeldtSykmelding(UUID.randomUUID(), "fnr", null, Periode(LocalDate.now().minusDays(10), LocalDate.now())))

            testDB.sykmeldingErAlleredeRegistrertForBruker("fnr") shouldEqual true
        }
    }
})
