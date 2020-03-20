package no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.client

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import java.time.LocalDate
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model.Arbeidsforhold

class ArbeidsforholdClient(private val httpClient: HttpClient, private val basePath: String) {

    private val arbeidsforholdPath = "$basePath/aareg-services/api/v1/arbeidstaker/arbeidsforhold"
    private val navPersonident = "Nav-Personident"
    private val navConsumerToken = "Nav-Consumer-Token"
    private val ansettelsesperiodeFom = "2020-03-16"
    private val ansettelsesperiodeFomQueryParam = "ansettelsesperiodeFom"
    private val ansettelsesperiodeTomQueryParam = "ansettelsesperiodeTom"
    private val sporingsinformasjon = "sporingsinformasjon"

    suspend fun getArbeidsforhold(fnr: String, token: String, stsToken: String): List<Arbeidsforhold> {
        val today = LocalDate.now().toString()
        return httpClient.get("$arbeidsforholdPath?" +
                "$ansettelsesperiodeFomQueryParam=$ansettelsesperiodeFom&" +
                "$ansettelsesperiodeTomQueryParam=$today&" +
                "$sporingsinformasjon=false") {
            header(navPersonident, fnr)
            header(HttpHeaders.Authorization, token)
            header(navConsumerToken, "Bearer $stsToken")
        }
    }
}
