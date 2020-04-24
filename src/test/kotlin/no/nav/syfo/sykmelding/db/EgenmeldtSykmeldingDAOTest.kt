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
import org.amshove.kluent.shouldBe
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
                Periode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1)),
                false)

        it("Should not overlap when not exists") {
            assertFalse(testDB.sykmeldingOverlapperGrenseverdi(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.fom, egenmeldtSykmelding.periode.fom))
        }

        it("Should insert") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)
        }

        it("Should insert correct with egenSykdom = false") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)
            val sykmelding = testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.fnr).get(0)
            sykmelding.egenSykdom shouldEqual false
        }

        it("Should insert correct with egenSykdom = true") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding.copy(egenSykdom = true))
            val sykmelding = testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.fnr).get(0)
            sykmelding.egenSykdom shouldEqual true
        }

        it("Should overlap on duplicate insert") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

            assertTrue(testDB.sykmeldingOverlapperGrenseverdi(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.fom, egenmeldtSykmelding.periode.fom))
        }

        it("Should overlap on insert within 16 days") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

            testDB.sykmeldingOverlapperGrenseverdi(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.fom.plusDays(16), egenmeldtSykmelding.periode.tom.plusDays(16)) shouldBe true
            testDB.sykmeldingOverlapperGrenseverdi(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.fom.minusDays(16), egenmeldtSykmelding.periode.tom.minusDays(16)) shouldBe true
            testDB.sykmeldingOverlapperGrenseverdi(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.fom.plusDays(17), egenmeldtSykmelding.periode.tom.plusDays(17)) shouldBe false
        }

        it("Should overlap on insert within 16 days - multiple records") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)
            val plus16 = egenmeldtSykmelding.copy(id = UUID.randomUUID(), periode = Periode(egenmeldtSykmelding.periode.tom, egenmeldtSykmelding.periode.tom.plusDays(16)));
            testDB.registrerEgenmeldtSykmelding(plus16)

            testDB.sykmeldingOverlapperGrenseverdi(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.fom.plusDays(16), egenmeldtSykmelding.periode.tom.plusDays(16)) shouldBe true
            testDB.sykmeldingOverlapperGrenseverdi(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.fom.minusDays(16), egenmeldtSykmelding.periode.tom.minusDays(16)) shouldBe true

            // Overlapper med plus16
            testDB.sykmeldingOverlapperGrenseverdi(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.fom.plusDays(17), egenmeldtSykmelding.periode.tom.plusDays(17)) shouldBe true
        }

        it("Should count number of records") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)
            testDB.antallSykmeldingerInnenforPeriode(egenmeldtSykmelding.fnr, egenmeldtSykmelding.periode.tom.minusMonths(4), egenmeldtSykmelding.periode.tom) shouldBe 1

            val nySykmelding = EgenmeldtSykmelding(UUID.randomUUID(),
                    "12345678912",
                    null,
                    Periode(fom = LocalDate.now().plusDays(20), tom = LocalDate.now().plusDays(36)),
                    true)
            testDB.registrerEgenmeldtSykmelding(nySykmelding)
            testDB.antallSykmeldingerInnenforPeriode(egenmeldtSykmelding.fnr, nySykmelding.periode.tom.minusMonths(4), nySykmelding.periode.tom) shouldBe 2
        }

        it("Should  query by id") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

            val res = testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.id)

            assertEquals(egenmeldtSykmelding, res)
        }

        it("Should  query by fødselsnummer") {
            testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)

            val res = testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.fnr).get(0)

            assertEquals(egenmeldtSykmelding, res)
        }
    }

    describe("Verifisering av duplikatsjekk") {
        it("sykmeldingErAlleredeRegistrert skal returnere false når sykmelding ikke er registrert") {
            testDB.sykmeldingErAlleredeRegistrertForBruker("fnr") shouldEqual false
        }

        it("sykmeldingErAlleredeRegistrert skal returnere true når en lik sykmelding er registrert fra før") {
            testDB.registrerEgenmeldtSykmelding(EgenmeldtSykmelding(UUID.randomUUID(), "fnr", null, Periode(LocalDate.now().minusDays(10), LocalDate.now()), false))

            testDB.sykmeldingErAlleredeRegistrertForBruker("fnr") shouldEqual true
        }
    }
})
