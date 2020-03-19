package no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model

import java.time.LocalDate

data class Gyldighetsperiode(
    val fom: LocalDate?,
    val tom: LocalDate?
)
