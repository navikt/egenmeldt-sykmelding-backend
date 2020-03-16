package no.nav.syfo.sykmelding.model

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
)
