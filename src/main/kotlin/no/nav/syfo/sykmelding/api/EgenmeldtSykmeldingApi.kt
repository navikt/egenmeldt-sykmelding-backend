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
import java.util.UUID
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
            val egenmeldtSykmeldingRequest = call.receive<EgenmeldtSykmeldingRequest>()
            egenmeldtSykmeldingService.validerOgRegistrerEgenmeldtSykmelding(sykmeldingRequest = overstyrArbeidsforhold(egenmeldtSykmeldingRequest), fnr = fnr, userToken = token, callId = callId)
            call.respond(HttpStatusCode.Created)
        }
    }
}

// Kun tilgjengelig hvis man ikke har arbeidsforhold p.t.
fun overstyrArbeidsforhold(egenmeldtSykmeldingRequest: EgenmeldtSykmeldingRequest): EgenmeldtSykmeldingRequest {
    val originalPeriode = egenmeldtSykmeldingRequest.periode
    return egenmeldtSykmeldingRequest.copy(
        arbeidsforhold = emptyList(),
        periode = originalPeriode.copy(
            fom = originalPeriode.fom,
            tom = originalPeriode.tom
        )
    )
}
