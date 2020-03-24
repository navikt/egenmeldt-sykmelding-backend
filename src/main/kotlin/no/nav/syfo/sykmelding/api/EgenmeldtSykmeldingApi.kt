package no.nav.syfo.sykmelding.api

import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import javax.jms.MessageProducer
import javax.jms.Session
import no.nav.syfo.metrics.EGENMELDT_SYKMELDING_HTTP_REQ_COUNTER
import no.nav.syfo.sykmelding.model.EgenmeldtSykmeldingRequest
import no.nav.syfo.sykmelding.service.EgenmeldtSykmeldingService

@KtorExperimentalAPI
fun Route.registrerEgenmeldtSykmeldingApi(egenmeldtSykmeldingService: EgenmeldtSykmeldingService, session: Session, syfoserviceProducer: MessageProducer) {

    route("api/v1/sykmelding/egenmeldt") {
        post {
            EGENMELDT_SYKMELDING_HTTP_REQ_COUNTER.inc()
            val principal: JWTPrincipal = call.authentication.principal()!!
            val fnr = principal.payload.subject
            val egenmeldtSykmeldingRequest = call.receive<EgenmeldtSykmeldingRequest>()
            egenmeldtSykmeldingService.registrerEgenmeldtSykmelding(egenmeldtSykmeldingRequest, fnr, session, syfoserviceProducer)
            call.respond(HttpStatusCode.Created)
        }
    }
}
