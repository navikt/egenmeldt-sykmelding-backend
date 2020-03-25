package no.nav.syfo.sykmelding.errorhandling

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.sykmelding.errorhandling.exceptions.IkkeTilgangException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException

fun StatusPages.Configuration.setUpSykmeldingExceptionHandler() {
    exception<TomBeforeFomDateException> {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError(it.message))))
    }
    exception<PersonNotFoundInPdl> {
        call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf(EgenmeldtSykmeldingError(it.message))))
    }
    exception<IkkeTilgangException> {
        call.respond(HttpStatusCode.Forbidden, ErrorResponse(listOf(EgenmeldtSykmeldingError(it.message))))
    }
}
