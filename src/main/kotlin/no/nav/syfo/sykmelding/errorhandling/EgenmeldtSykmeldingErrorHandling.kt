package no.nav.syfo.sykmelding.errorhandling

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException

fun StatusPages.Configuration.setUpSykmeldingExceptionHandler() {
    exception<TomBeforeFomDateException> {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError(it.message))))
    }
}
