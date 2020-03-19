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

class EgenmeldtSykmeldingDAOTest : Spek({

    val testDB = TestDB()

    describe("EgenmeldtSykmeldingDAO test") {
        val egenmeldtSykmelding = EgenmeldtSykmelding(UUID.randomUUID(),
                "12345678912",
                Arbeidsforhold("arbeidsgiver", "123456789", 50.5),
                Periode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(1)))

        it("Should insert") {
            runBlocking {
                testDB.registrerEgenmeldtSykmelding(egenmeldtSykmelding)
            }
        }
        it("Should  query ") {
            runBlocking {
                testDB.finnEgenmeldtSykmelding(egenmeldtSykmelding.id)
            }
        }
    }
})