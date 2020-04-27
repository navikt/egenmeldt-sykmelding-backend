package no.nav.syfo.sykmelding.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.sql.Date
import java.sql.ResultSet
import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.log
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.Periode
import org.postgresql.util.PGobject
import java.time.LocalDate

fun DatabaseInterface.registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmelding) {
    connection.use { connection ->

        val insertQuery: String =
                """
                    INSERT INTO egenmeldt_sykmelding (
                        id, 
                        pasientfnr, 
                        fom, 
                        tom, 
                        arbeidsforhold,
                        egenSykdom) 
                    VALUES (?, ?, ?, ?, ?, ?);
                """

        var i = 1

        connection.prepareStatement(insertQuery).use {
            it.setObject(i++, egenmeldtSykmelding.id)
            it.setString(i++, egenmeldtSykmelding.fnr)
            it.setDate(i++, Date.valueOf(egenmeldtSykmelding.periode.fom))
            it.setDate(i++, Date.valueOf(egenmeldtSykmelding.periode.tom))
            it.setObject(i++, PGobject().also {
                it.type = "json"
                it.value = jacksonObjectMapper().writeValueAsString(egenmeldtSykmelding.arbeidsforhold)
            })
            it.setBoolean(i++, egenmeldtSykmelding.egenSykdom)
            it.execute()
        }

        connection.commit()
    }
}

/**
 * Måler avstand fra parameter fom og tom, mot eksisterende fom og tom i databasen
 * Hvis avstand er mindre enn grenseverdien antallDager vil metoden returnere true
 *
 * Denne kan brukes til å vurdere om en ny egenmeldt sykmelding er tilstrekkelig lang unna
 * eksisterende sykmeldinger, per i dag 16 dager.
 */
fun DatabaseInterface.sykmeldingOverlapperGrenseverdi(fnr: String, fom: LocalDate, tom: LocalDate, antallDager: Int = 16): Boolean {
    connection.use { connection ->
        var i = 1;
        connection.prepareStatement(
                """
                    SELECT abs(? - fom) as distance_fom_fom,
                           abs(? - fom) as distance_tom_fom, 
                           abs(? - tom) as distance_fom_tom, 
                           abs(? - tom) as distance_tom_tom
                    FROM egenmeldt_sykmelding
                    WHERE pasientfnr = ?;
        """
        ).use {
            it.setDate(i++, Date.valueOf(fom))
            it.setDate(i++, Date.valueOf(tom))
            it.setDate(i++, Date.valueOf(fom))
            it.setDate(i++, Date.valueOf(tom))
            it.setString(i++, fnr)
            val executeQuery = it.executeQuery()
            while(executeQuery.next()) {
                val distanceFomFom = executeQuery.getInt("distance_fom_fom")
                val distanceTomFom = executeQuery.getInt("distance_tom_fom")
                val distanceFomTom = executeQuery.getInt("distance_fom_tom")
                val distanceTomTom = executeQuery.getInt("distance_tom_tom")

                if(distanceFomFom < antallDager || distanceTomFom < antallDager || distanceFomTom < antallDager || distanceTomTom < antallDager) {
                    return true
                }
            }
            return false
        }
    }
}

fun DatabaseInterface.antallSykmeldingerInnenforPeriode(fnr: String, fom: LocalDate, tom: LocalDate): Int {
    connection.use { connection ->
        var i = 1;
        connection.prepareStatement(
                """
                SELECT count(*) as count
                FROM egenmeldt_sykmelding
                WHERE pasientfnr = ?
                AND (fom between ? AND ?
                  OR tom between ? AND ?);
        """
        ).use {
            it.setString(i++, fnr)
            it.setDate(i++, Date.valueOf(fom))
            it.setDate(i++, Date.valueOf(tom))
            it.setDate(i++, Date.valueOf(fom))
            it.setDate(i++, Date.valueOf(tom))
            val executeQuery = it.executeQuery()
            if (executeQuery.next()) {
                return executeQuery.getInt("count")
            }
            return 0
        }
    }
}

fun DatabaseInterface.sykmeldingErAlleredeRegistrertForBruker(fnr: String): Boolean =
    connection.use { connection ->
        connection.prepareStatement(
            """
               SELECT 1 
               FROM egenmeldt_sykmelding 
               WHERE pasientfnr = ?
            """
        ).use {
            it.setString(1, fnr)
            it.executeQuery().next()
        }
    }

fun DatabaseInterface.finnEgenmeldtSykmelding(id: UUID): EgenmeldtSykmelding? {
    connection.use { connection ->

        val query =
                """
                    SELECT * 
                    FROM egenmeldt_sykmelding 
                    WHERE id = ? """

        connection.prepareStatement(query).use {
            it.setObject(1, id)
            return it.executeQuery().toList { tilEgenmeldtSykmelding() }.firstOrNull()
        }
    }
}

fun DatabaseInterface.slettEgenmeldtSykmelding(id: UUID) {
    connection.use { connection ->
        log.info("Sletter innslag for egenmeldt sykmelding med id: {}", id.toString())
        connection.prepareStatement(
            """
                DELETE FROM egenmeldt_sykmelding
                WHERE id=?;
            """
        ).use {
            it.setObject(1, id)
            it.execute()
        }
        connection.commit()
    }
}

fun DatabaseInterface.finnEgenmeldtSykmelding(pasientfnr: String): List<EgenmeldtSykmelding> {
    connection.use { connection ->

        val query =
                """ SELECT * 
                    FROM egenmeldt_sykmelding 
                    WHERE pasientfnr = ? """

        connection.prepareStatement(query).use {
            it.setObject(1, pasientfnr)
            return it.executeQuery().toList { tilEgenmeldtSykmelding() }
        }
    }
}

fun ResultSet.tilEgenmeldtSykmelding(): EgenmeldtSykmelding {
    return EgenmeldtSykmelding(
            id = getObject("id") as UUID,
            fnr = getString("pasientfnr"),
            arbeidsforhold = jacksonObjectMapper().readValue(getString("arbeidsforhold")),
            periode = Periode(
                    getDate("fom").toLocalDate(),
                    getDate("tom").toLocalDate()),
            egenSykdom = getBoolean("egenSykdom"))
}
