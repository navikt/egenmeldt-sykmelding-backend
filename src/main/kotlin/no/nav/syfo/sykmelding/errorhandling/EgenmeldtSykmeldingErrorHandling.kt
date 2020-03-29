package no.nav.syfo.sykmelding.errorhandling

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.sykmelding.errorhandling.exceptions.AktoerNotFoundException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForLangPeriodeException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForTidligsteFomException
import no.nav.syfo.sykmelding.errorhandling.exceptions.IkkeTilgangException
import no.nav.syfo.sykmelding.errorhandling.exceptions.OverlappMedEksisterendeSykmeldingException
import no.nav.syfo.sykmelding.errorhandling.exceptions.SykmeldingAlreadyExistsException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException

fun StatusPages.Configuration.setUpSykmeldingExceptionHandler() {
    exception<TomBeforeFomDateException> {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError("TOM_ER_FOR_FOM", it.message))))
    }
    exception<ForLangPeriodeException> {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError("FOR_LANG_PERIODE", it.message))))
    }
    exception<ForTidligsteFomException> {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError("FOM_BEFORE_VALID_DATE", it.message))))
    }
    exception<OverlappMedEksisterendeSykmeldingException> {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError("OVERLAPPER_MED_ANDRE_SYKMELDINGSPERIODER", it.message))))
    }
    exception<SykmeldingAlreadyExistsException> {
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError("HAR_ALLEREDE_EGENMELDT_SYKMELDING", it.message))))
    }
    exception<PersonNotFoundInPdl> {
        call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf(EgenmeldtSykmeldingError("PERSON_NOT_FOUND", it.message))))
    }
    exception<AktoerNotFoundException> {
        call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf(EgenmeldtSykmeldingError("AKTOR_NOT_FOUND", it.message))))
    }
    exception<IkkeTilgangException> {
        call.respond(HttpStatusCode.Forbidden, ErrorResponse(listOf(EgenmeldtSykmeldingError("FORBIDDEN", it.message))))
    }
}
