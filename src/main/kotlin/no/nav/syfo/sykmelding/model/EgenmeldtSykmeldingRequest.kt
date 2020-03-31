package no.nav.syfo.sykmelding.model

data class EgenmeldtSykmeldingRequest(
    val periode: Periode,
    val egenSykdom: Boolean,
    val arbeidsforhold: List<Arbeidsforhold>
)
