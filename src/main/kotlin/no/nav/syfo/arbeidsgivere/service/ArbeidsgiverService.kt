package no.nav.syfo.arbeidsgivere.service

import java.time.LocalDate
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.client.ArbeidsforholdClient
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model.Arbeidsavtale
import no.nav.syfo.arbeidsgivere.integration.arbeidsforhold.model.Arbeidsforhold
import no.nav.syfo.arbeidsgivere.integration.organisasjon.client.OrganisasjonsinfoClient
import no.nav.syfo.arbeidsgivere.integration.organisasjon.model.Navn
import no.nav.syfo.arbeidsgivere.integration.organisasjon.model.Organisasjonsinfo
import no.nav.syfo.arbeidsgivere.model.Arbeidsgiverinfo
import no.nav.syfo.client.StsOidcClient

class ArbeidsgiverService(private val arbeidsforholdClient: ArbeidsforholdClient, private val organisasjonsinfoClient: OrganisasjonsinfoClient, private val stsOidcClient: StsOidcClient) {
    suspend fun getArbeidsgivere(fnr: String, token: String, date: LocalDate): List<Arbeidsgiverinfo> {
        val stsToken = stsOidcClient.oidcToken()
        val arbeidsgivere = arbeidsforholdClient.getArbeidsforhold(fnr, token, stsToken.access_token)
        val arbeidsgiverList = ArrayList<Arbeidsgiverinfo>()
        arbeidsgivere.filter {
            it.arbeidsgiver.type == "Organisasjon"
        }.forEach { arbeidsforhold ->
            val organisasjonsinfo = organisasjonsinfoClient.getOrginfo(arbeidsforhold.arbeidsgiver.organisasjonsnummer!!)
            arbeidsforhold.arbeidsavtaler.asSequence().filter {
                checkGyldighetsperiode(it, date)
            }.forEach { arbeidsavtale ->
                addArbeidsinfo(arbeidsgiverList, organisasjonsinfo, arbeidsavtale, arbeidsforhold)
            }
        }
        return arbeidsgiverList
    }

    private fun addArbeidsinfo(arbeidsgiverList: ArrayList<Arbeidsgiverinfo>, organisasjonsinfo: Organisasjonsinfo, arbeidsavtale: Arbeidsavtale, arbeidsforhold: Arbeidsforhold) {
        arbeidsgiverList.add(Arbeidsgiverinfo(
                orgNummer = organisasjonsinfo.organisasjonsnummer,
                juridiskOrgNummer = arbeidsforhold.opplysningspliktig.organisasjonsnummer!!,
                navn = getName(organisasjonsinfo.navn),
                Stillingsprosent = arbeidsavtale.stillingsprosent
        ))
    }

    private fun checkGyldighetsperiode(it: Arbeidsavtale, date: LocalDate): Boolean {
        val fom = it.gyldighetsperiode.fom
        val tom = it.gyldighetsperiode.tom
        val tomIsNullOrBeforeNow = !(tom?.isBefore(date) ?: false)
        val fomIsNullOrAfterNow = !(fom?.isAfter(date) ?: true)
        return tomIsNullOrBeforeNow && fomIsNullOrAfterNow
    }

    fun getName(navn: Navn): String {
        val builder = StringBuilder()
        if (!navn.navnelinje1.isNullOrBlank()) {
            builder.appendln(navn.navnelinje1)
        }
        if (!navn.navnelinje2.isNullOrBlank()) {
            builder.appendln(navn.navnelinje2)
        }
        if (!navn.navnelinje3.isNullOrBlank()) {
            builder.appendln(navn.navnelinje3)
        }
        if (!navn.navnelinje4.isNullOrBlank()) {
            builder.appendln(navn.navnelinje4)
        }
        if (!navn.navnelinje5.isNullOrBlank()) {
            builder.appendln(navn.navnelinje5)
        }
        return builder.lineSequence().filter {
                    it.isNotBlank()
                }.joinToString(separator = ",")
    }
}
