package no.nav.syfo.sykmelding.db

import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.Periode
import no.nav.syfo.sykmelding.util.TestDB
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EgenmeldtSykmeldingDAOTest : Spek({

    val testDB = TestDB()

    describe("EgenmeldtSykmeldingDAO test") {
        val egenmeldtSykmelding = EgenmeldtSykmelding(UUID.randomUUID(),
                "12345678912",
                Arbeidsforhold("arbeidsgiver", "123456789", 50.5),
                Periode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1)))

        it("Should not overlap when not exists") {
            runBlocking {
                assertFalse(testDB.sykmeldingOverlapper(egenmeldtSykmelding))
            }
        }

        it("Should insert") {
            runBlocking {
                testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)
            }
        }

        it("Should overlap after insert") {
            runBlocking {
                assertTrue(testDB.sykmeldingOverlapper(egenmeldtSykmelding))
            }
        }
        it("Should  query by id") {
            runBlocking {
                val res = testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.id)
                assertEquals(egenmeldtSykmelding, res)
            }
        }

        it("Should  query by fødselsnummer") {
            runBlocking {
                val res = testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.fodselsnummer)
                assertEquals(egenmeldtSykmelding, res)
            }
        }

    }
})