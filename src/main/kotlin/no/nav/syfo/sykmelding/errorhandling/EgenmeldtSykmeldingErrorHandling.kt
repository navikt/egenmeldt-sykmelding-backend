package no.nav.syfo.sykmelding.errorhandling

import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import no.nav.syfo.metrics.ERROR_HIT_COUNTER
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.sykmelding.errorhandling.exceptions.AktoerNotFoundException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForLangPeriodeException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForMangeEgenmeldingerException
import no.nav.syfo.sykmelding.errorhandling.exceptions.ForTidligsteFomException
import no.nav.syfo.sykmelding.errorhandling.exceptions.IkkeTilgangException
import no.nav.syfo.sykmelding.errorhandling.exceptions.OverlappMedEksisterendeSykmeldingException
import no.nav.syfo.sykmelding.errorhandling.exceptions.TomBeforeFomDateException

private val TOM_ER_FOR_FOM = "TOM_ER_FOR_FOM"
private val FOR_LANG_PERIODE = "FOR_LANG_PERIODE"
private val FOM_BEFORE_VALID_DATE = "FOM_BEFORE_VALID_DATE"
private val OVERLAPPER_MED_ANDRE_SYKMELDINGSPERIODER = "OVERLAPPER_MED_ANDRE_SYKMELDINGSPERIODER"
private val FOR_MANGE_SYKMELDINGER_I_PERIODE = "FOR_MANGE_SYKMELDINGER_I_PERIODE"
private val PERSON_NOT_FOUND = "PERSON_NOT_FOUND"
private val AKTOR_NOT_FOUND = "AKTOR_NOT_FOUND"
private val FORBIDDEN = "FORBIDDEN"

fun StatusPages.Configuration.setUpSykmeldingExceptionHandler() {
    exception<TomBeforeFomDateException> {
        ERROR_HIT_COUNTER.labels(TOM_ER_FOR_FOM).inc()
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError(TOM_ER_FOR_FOM, it.message))))
    }
    exception<ForLangPeriodeException> {
        ERROR_HIT_COUNTER.labels(FOR_LANG_PERIODE).inc()
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError(FOR_LANG_PERIODE, it.message))))
    }
    exception<ForTidligsteFomException> {
        ERROR_HIT_COUNTER.labels(FOM_BEFORE_VALID_DATE).inc()
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError(FOM_BEFORE_VALID_DATE, it.message))))
    }
    exception<OverlappMedEksisterendeSykmeldingException> {
        ERROR_HIT_COUNTER.labels(OVERLAPPER_MED_ANDRE_SYKMELDINGSPERIODER).inc()
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError(OVERLAPPER_MED_ANDRE_SYKMELDINGSPERIODER, it.message))))
    }
    exception<ForMangeEgenmeldingerException> {
        ERROR_HIT_COUNTER.labels(FOR_MANGE_SYKMELDINGER_I_PERIODE).inc()
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(listOf(EgenmeldtSykmeldingError(FOR_MANGE_SYKMELDINGER_I_PERIODE, it.message))))
    }
    exception<PersonNotFoundInPdl> {
        ERROR_HIT_COUNTER.labels(PERSON_NOT_FOUND).inc()
        call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf(EgenmeldtSykmeldingError(PERSON_NOT_FOUND, it.message))))
    }
    exception<AktoerNotFoundException> {
        ERROR_HIT_COUNTER.labels(AKTOR_NOT_FOUND).inc()
        call.respond(HttpStatusCode.NotFound, ErrorResponse(listOf(EgenmeldtSykmeldingError(AKTOR_NOT_FOUND, it.message))))
    }
    exception<IkkeTilgangException> {
        ERROR_HIT_COUNTER.labels(FORBIDDEN).inc()
        call.respond(HttpStatusCode.Forbidden, ErrorResponse(listOf(EgenmeldtSykmeldingError(FORBIDDEN, it.message))))
    }
}
