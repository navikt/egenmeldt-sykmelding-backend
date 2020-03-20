package no.nav.syfo.arbeidsgivere.integration.organisasjon.client

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import no.nav.syfo.arbeidsgivere.integration.organisasjon.model.Organisasjonsinfo

class OrganisasjonsinfoClient(private val httpClient: HttpClient, private val basePath: String) {
    suspend fun getOrginfo(orgNummer: String): Organisasjonsinfo {
        return httpClient.get("$basePath/ereg/api/v1/organisasjon/$orgNummer/noekkelinfo")
    }
}
