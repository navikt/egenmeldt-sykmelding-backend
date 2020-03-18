package no.nav.syfo.sykmelding.model

data class EgenmeldtSykmeldingRequest(
    val periode: Periode,
    val arbeidsforhold: List<Arbeidsforhold>

)
