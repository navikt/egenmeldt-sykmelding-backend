package no.nav.syfo.syfosmregister.model

import java.time.LocalDate

data class SykmeldingsperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: GradertDTO?
)
