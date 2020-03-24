package no.nav.syfo.pdl.service

import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.client.model.Adressebeskyttelse
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.pdl.model.Navn
import no.nav.syfo.pdl.model.PdlPerson
import org.slf4j.LoggerFactory

class PdlPersonService(private val pdlClient: PdlClient, val stsOidcClient: StsOidcClient) {
    companion object {
        const val STRENGT_FORTROLIG_UTLAND = "STRENGT_FORTROLIG_UTLAND"
        const val STRENGT_FORTROLIG = "STRENGT_FORTROLIG"
        private val log = LoggerFactory.getLogger(PdlPersonService::class.java)
    }
    suspend fun getPersonOgDiskresjonskode(fnr: String, userToken: String): PdlPerson {
        val stsToken = stsOidcClient.oidcToken().access_token
        val pdlResponse = pdlClient.getPerson(fnr = fnr, token = userToken, stsToken = stsToken)
        if (pdlResponse.data.hentPerson == null) {
            log.error("Fant ikke person i PDL")
            throw PersonNotFoundInPdl("Fant ikke person i PDL")
        }
        if (pdlResponse.data.hentPerson.navn.isNullOrEmpty()) {
            log.error("Fant ikke navn på person i PDL")
            throw PersonNotFoundInPdl("Fant ikke navn på person i PDL")
        }
        if (pdlResponse.data.hentPerson.adressebeskyttelse.isNullOrEmpty()) {
            log.error("Fant ikke diskresjonskode i PDL")
            throw PersonNotFoundInPdl("Fant ikke diskresjonskode i PDL")
        }
        return PdlPerson(getNavn(pdlResponse.data.hentPerson.navn[0]), hasFortroligAdresse(pdlResponse.data.hentPerson.adressebeskyttelse[0]))
    }

    private fun hasFortroligAdresse(adressebeskyttelse: Adressebeskyttelse): Boolean {
        return when (adressebeskyttelse.gradering) {
            STRENGT_FORTROLIG_UTLAND -> true
            STRENGT_FORTROLIG -> true
            else -> false
        }
    }

    private fun getNavn(navn: no.nav.syfo.pdl.client.model.Navn): Navn {
        return Navn(fornavn = navn.fornavn, mellomnavn = navn.mellomnavn, etternavn = navn.etternavn)
    }
}
