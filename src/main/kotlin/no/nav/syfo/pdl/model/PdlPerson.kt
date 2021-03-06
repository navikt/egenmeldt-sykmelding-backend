package no.nav.syfo.pdl.model

data class PdlPerson(
    val navn: Navn,
    val fortroligAdresse: Boolean,
    val aktorId: String
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)
