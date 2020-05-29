package no.nav.syfo.sykmelding.api

import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDate
import java.util.UUID
import no.nav.syfo.log
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_HTTP_REQ_COUNTER
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.service.EgenmeldtSykmeldingService

@KtorExperimentalAPI
fun Route.registrerEgenmeldtSykmeldingApi(egenmeldtSykmeldingService: EgenmeldtSykmeldingService) {

    route("api/v1/sykmelding/egenmeldt") {
        post {
            EGENMELDT_SYKMELDING_HTTP_REQ_COUNTER.inc()
            val principal: JWTPrincipal = call.authentication.principal()!!
            val fnr = principal.payload.subject
            val token = call.request.headers[HttpHeaders.Authorization]!!
            val callId = UUID.randomUUID().toString()
            if (erTilgjengelig(now = LocalDate.now())) {
                val egenmeldtSykmeldingRequest = call.receive<EgenmeldtSykmeldingRequest>()
                egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(sykmeldingRequest = overstyrArbeidsforholdOgPeriode(egenmeldtSykmeldingRequest), fnr = fnr, userToken = token, callId = callId)
                call.respond(HttpStatusCode.Created)
            } else {
                log.warn("Egenmeldingen er ikke lenger tilgjengelig")
                call.respond(HttpStatusCode.ServiceUnavailable, "Egenmeldingen er ikke lenger tilgjengelig")
            }
        }
    }
}

var ikkeTilgjengeligFra: LocalDate = LocalDate.of(2020, 6, 1)

fun erTilgjengelig(now: LocalDate): Boolean {
    if (now.isBefore(ikkeTilgjengeligFra)) {
        return true
    }
    return false
}

// Kun tilgjengelig hvis man ikke har arbeidsforhold p.t., og nøyaktig 16 dager fra dagens dato
fun overstyrArbeidsforholdOgPeriode(egenmeldtSykmeldingRequest: EgenmeldtSykmeldingRequest): EgenmeldtSykmeldingRequest {
    val originalPeriode = egenmeldtSykmeldingRequest.periode
    return egenmeldtSykmeldingRequest.copy(
        arbeidsforhold = emptyList(),
        periode = originalPeriode.copy(
            fom = LocalDate.now(),
            tom = LocalDate.now().plusDays(15)
        )
    )
}
