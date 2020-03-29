package no.nav.syfo.syfosmregister.client

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import java.time.LocalDate
import no.nav.syfo.syfosmregister.model.SykmeldingDTO

class SyfosmregisterSykmeldingClient(private val httpClient: HttpClient, private val baseUrl: String) {
    suspend fun getSykmeldinger(token: String, fom: LocalDate, tom: LocalDate?): List<SykmeldingDTO> {
        return httpClient.get(getRequestUrl(fom, tom)) {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", token)
            }
        }
    }

    fun getRequestUrl(fom: LocalDate, tom: LocalDate?): String {
        return "$baseUrl/api/v2/sykmeldinger?exclude=AVBRUTT&fom=$fom" + tom?.let { "&tom=$it" }.orEmpty()
    }
}
