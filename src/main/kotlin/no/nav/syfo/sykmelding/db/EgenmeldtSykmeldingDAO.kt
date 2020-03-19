package no.nav.syfo.sykmelding.db

import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.EgenmeldtSykmelding
import no.nav.syfo.sykmelding.model.Periode
import java.sql.Date
import java.sql.ResultSet

fun DatabaseInterface.registrerEgenmeldtSykmelding(egenmeldtSykmelding: EgenmeldtSykmelding) {
    connection.use { connection ->

        val insertQuery: String =
                """
                    INSERT INTO egenmeldt_sykmelding (
                        id, 
                        pasientfnr, 
                        fom, 
                        tom, 
                        arbeidsforhold_navn,
                        arbeidsforhold_orgnr,
                        arbeidsforhold_stillingsprosent) 
                    VALUES (?, ?, ?, ?, ?, ?, ?);
                """

        var i = 1

        connection.prepareStatement(insertQuery).use {
            it.setObject(i++, egenmeldtSykmelding.id)
            it.setString(i++, egenmeldtSykmelding.fodselsnummer)
            it.setDate(i++, Date.valueOf(egenmeldtSykmelding.periode.tom))
            it.setDate(i++, Date.valueOf(egenmeldtSykmelding.periode.fom))
            it.setString(i++, egenmeldtSykmelding.arbeidsforhold.navn)
            it.setString(i++, egenmeldtSykmelding.arbeidsforhold.orgNummer)
            it.setDouble(i++, egenmeldtSykmelding.arbeidsforhold.stillingsprosent)
        }
        connection.commit()
    }
}

fun DatabaseInterface.finnEgenmeldtSykmelding(id: UUID) {


    connection.use { connection ->

        val query: String =
                """
                    SELECT * 
                    FROM egenmeldt_sykmelding 
                    WHERE id = ?
                """

        connection.prepareStatement(query).use {
            it.setObject(1, id)
            it.executeQuery()
                    .toList { tilEgenmeldtSykmelding() }
        }

        // TODO: Lag objekt og returner

        TODO("Må implementeres")
    }

}

fun ResultSet.tilEgenmeldtSykmelding(): EgenmeldtSykmelding {
    return EgenmeldtSykmelding(
            id = getObject("id") as UUID,
            fodselsnummer = getString("fodselsnummer"),
            arbeidsforhold = Arbeidsforhold(
                    getString("arbeidsforhold_navn"),
                    getString("arbeidsforhold_orgnr"),
                    getDouble("arbeidsforhold_stillingsprosent")),
            periode = Periode(
                    getDate("fom").toLocalDate(),
                    getDate("tom").toLocalDate()))
}