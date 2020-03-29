package no.nav.syfo.sykmelding.errorhandling

data class EgenmeldtSykmeldingError(
    val errorCode: String,
    val description: String
)
