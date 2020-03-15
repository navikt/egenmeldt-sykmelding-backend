package no.nav.syfo.sykmelding.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.service.EgenmeldtSykmeldingService

fun Route.registrerEgenmeldtSykmeldingApi(egenmeldtSykmeldingService: EgenmeldtSykmeldingService) {

    route("api/v1/sykmelding/egenmeldt") {
        post {
            val egenmeldtSykmeldingRequest = call.receive<EgenmeldtSykmeldingRequest>()
            egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest)
            call.respond(HttpStatusCode.Created)
        }
    }
}