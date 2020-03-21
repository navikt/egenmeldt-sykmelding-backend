package no.nav.syfo.sykmelding.model

data class Pasient(
    val fnr: String,
    val aktorId: String,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
)
