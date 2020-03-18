package no.nav.syfo.sykmelding.model

import java.util.UUID

data class EgenmeldtSykmeldingRequest(
    val periode: Periode,
    val arbeidsforhold: List<Arbeidsforhold>
)
