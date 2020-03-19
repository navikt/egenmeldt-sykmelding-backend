package no.nav.syfo.arbeidsgivere.service

import java.time.LocalDate
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model.Arbeidsavtale
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model.Arbeidsforhold
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model.Arbeidsgiver
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model.Gyldighetsperiode
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model.Opplysningspliktig
import no.nav.syfo.arbeidsgivere.integration.organisasjon.model.Navn
import no.nav.syfo.arbeidsgivere.integration.organisasjon.model.Organisasjonsinfo

fun getArbeidsgiverforhold(gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(LocalDate.now(), LocalDate.now())): List<Arbeidsforhold> {
    return listOf(
            Arbeidsforhold(
                    Arbeidsgiver("Organisasjon", "123456789"),
                    Opplysningspliktig("Organisasjon", "987654321"),
                    listOf(
                            Arbeidsavtale(gyldighetsperiode = gyldighetsperiode, stillingsprosent = 100.0)
                    )
            )
    )
}

fun getOrganisasjonsinfo(): Organisasjonsinfo {
    return Organisasjonsinfo("123456789",
            Navn(
                    "Navn 1",
                    null,
                    null,
                    null,
                    null,
                    null)
    )
}
