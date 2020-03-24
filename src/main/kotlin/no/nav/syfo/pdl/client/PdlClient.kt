package no.nav.syfo.pdl.client

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import no.nav.syfo.objectMapper
import no.nav.syfo.pdl.client.model.GetPersonRequest
import no.nav.syfo.pdl.client.model.GetPersonResponse
import no.nav.syfo.pdl.client.model.GetPersonVeriables

class PdlClient(
    private val httpClient: HttpClient,
    private val basePath: String,
    private val graphQlQuery: String
) {

    private val navConsumerToken = "Nav-Consumer-Token"
    private val temaHeader = "TEMA"
    private val tema = "SYM"

    suspend fun getPerson(fnr: String, token: String, stsToken: String): GetPersonResponse {
        val getPersonRequest = GetPersonRequest(query = graphQlQuery, variables = GetPersonVeriables(ident = fnr))
        return httpClient.post(basePath) {
            body = objectMapper.writeValueAsString(getPersonRequest).replace(Regex("[\n\t]"), "")
            header(HttpHeaders.Authorization, token)
            header(temaHeader, tema)
            header(HttpHeaders.ContentType, "application/json")
            header(navConsumerToken, "Bearer $stsToken")
        }
    }
}
