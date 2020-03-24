package no.nav.syfo.sykmelding.integration.aktor.client

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.helpers.retry
import no.nav.syfo.log
import no.nav.syfo.sykmelding.errorhandling.exceptions.AktoerNotFoundException
import no.nav.syfo.sykmelding.integration.aktor.model.IdentInfoResult

@KtorExperimentalAPI
class AktoerIdClient(
    private val endpointUrl: String,
    private val stsClient: StsOidcClient,
    private val httpClient: HttpClient,
    private val serviceUserName: String
) {
    private suspend fun getAktoerIds(
        personNumbers: List<String>,
        msgId: String
    ): Map<String, IdentInfoResult> =
            retry("get_aktoerids") {
                httpClient.get<HttpResponse>("$endpointUrl/identer") {
                    accept(ContentType.Application.Json)
                    val oidcToken = stsClient.oidcToken()
                    headers {
                        append("Authorization", "Bearer ${oidcToken.access_token}")
                        append("Nav-Consumer-Id", serviceUserName)
                        append("Nav-Call-Id", msgId)
                        append("Nav-Personidenter", personNumbers.joinToString(","))
                    }
                    parameter("gjeldende", "true")
                    parameter("identgruppe", "AktoerId")
                }.call.response.receive<Map<String, IdentInfoResult>>()
            }

    suspend fun finnAktoerId(fnr: String, msgId: String): String {
        val aktoerIds = getAktoerIds(listOf(fnr), msgId)
        val patientIdents = aktoerIds[fnr]

        if (patientIdents == null || patientIdents.feilmelding != null) {
            log.error("Klarte ikke hente aktørIdent for fnr: $fnr og msgId $msgId")
            throw AktoerNotFoundException("Patient with fnr: $fnr not found in registry, error: $patientIdents.feilmelding")
        } else {
            return patientIdents.identer?.find { it.gjeldende }?.ident ?: throw AktoerNotFoundException("Patient with fnr: $fnr not found in registry, error: $patientIdents.feilmelding")
        }
    }
}
