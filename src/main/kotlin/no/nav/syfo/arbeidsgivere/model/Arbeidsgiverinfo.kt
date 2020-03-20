package no.nav.syfo.arbeidsgivere.model

data class Arbeidsgiverinfo(
    val orgNummer: String,
    val juridiskOrgNummer: String,
    val navn: String,
    val Stillingsprosent: Double
)
