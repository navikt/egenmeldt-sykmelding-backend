package no.nav.syfo.sykmelding.model

import java.util.UUID

data class EgenmeldtSykmelding(
    val id: UUID,
    val fnr: String,
    val arbeidsforhold: Arbeidsforhold?,
    val periode: Periode,
    val egenSykdom: Boolean
)
