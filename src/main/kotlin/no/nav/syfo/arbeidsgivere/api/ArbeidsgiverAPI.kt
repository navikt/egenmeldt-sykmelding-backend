package no.nav.syfo.arbeidsgivere.api

import io.ktor.application.call
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpHeaders
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import java.time.LocalDate
import no.nav.syfo.arbeidsgivere.service.ArbeidsgiverService

fun Route.registrerArbeidsgiverApi(arbeidsgiverService: ArbeidsgiverService) {
    route("api/v1/arbeidsforhold") {
        get {
            val principal = call.principal<JWTPrincipal>()!!
            val fnr = principal.payload.subject
            val token = call.request.headers.get(HttpHeaders.Authorization)!!
            call.respond(arbeidsgiverService.getArbeidsgivere(fnr, token, LocalDate.now()))
        }
    }
}
