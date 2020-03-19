package no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model

data class Arbeidsforhold(
    val arbeidsgiver: Arbeidsgiver,
    val opplysningspliktig: Opplysningspliktig,
    val arbeidsavtaler: List<Arbeidsavtale>
)
