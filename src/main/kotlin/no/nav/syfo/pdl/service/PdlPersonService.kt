package no.nav.syfo.pdl.service

import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.client.model.Adressebeskyttelse
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.pdl.model.Navn
import no.nav.syfo.pdl.model.PdlPerson
import no.nav.syfo.sykmelding.errorhandling.exceptions.AktoerNotFoundException
import org.slf4j.LoggerFactory

class PdlPersonService(private val pdlClient: PdlClient, val stsOidcClient: StsOidcClient) {
    companion object {
        const val STRENGT_FORTROLIG_UTLAND = "STRENGT_FORTROLIG_UTLAND"
        const val STRENGT_FORTROLIG = "STRENGT_FORTROLIG"
        private val log = LoggerFactory.getLogger(PdlPersonService::class.java)
    }
    suspend fun getPersonOgDiskresjonskode(fnr: String, userToken: String, callId: String): PdlPerson {
        val stsToken = stsOidcClient.oidcToken().access_token
        val pdlResponse = pdlClient.getPerson(fnr, userToken, stsToken)
        if (pdlResponse.data.hentPerson == null) {
            log.error("Fant ikke person i PDL {}", callId)
            throw PersonNotFoundInPdl("Fant ikke person i PDL")
        }
        if (pdlResponse.data.hentPerson.navn.isNullOrEmpty()) {
            log.error("Fant ikke navn på person i PDL {}", callId)
            throw PersonNotFoundInPdl("Fant ikke navn på person i PDL")
        }
        if (pdlResponse.data.hentIdenter == null || pdlResponse.data.hentIdenter.identer.isNullOrEmpty()) {
            log.error("Fant ikke aktørid i PDL {}", callId)
            throw AktoerNotFoundException("Fant ikke aktørId i PDL")
        }
        return PdlPerson(getNavn(pdlResponse.data.hentPerson.navn[0]), hasFortroligAdresse(pdlResponse.data.hentPerson.adressebeskyttelse), pdlResponse.data.hentIdenter.identer.first().ident)
    }

    private fun hasFortroligAdresse(adressebeskyttelse: List<Adressebeskyttelse>?): Boolean {
        return when {
            adressebeskyttelse.isNullOrEmpty() -> false
            adressebeskyttelse.any { it.gradering == STRENGT_FORTROLIG_UTLAND || it.gradering == STRENGT_FORTROLIG } -> true
            else -> false
        }
    }

    private fun getNavn(navn: no.nav.syfo.pdl.client.model.Navn): Navn {
        return Navn(fornavn = navn.fornavn, mellomnavn = navn.mellomnavn, etternavn = navn.etternavn)
    }
}
