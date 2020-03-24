package no.nav.syfo.pdl.service

import no.nav.syfo.pdl.client.model.Adressebeskyttelse
import no.nav.syfo.pdl.client.model.GetPersonResponse
import no.nav.syfo.pdl.client.model.HentPerson
import no.nav.syfo.pdl.client.model.IdentInformasjon
import no.nav.syfo.pdl.client.model.Identliste
import no.nav.syfo.pdl.client.model.Navn
import no.nav.syfo.pdl.client.model.ResponseData

fun getPdlResponse(adresseGradering: String): GetPersonResponse {
    return GetPersonResponse(ResponseData(
            hentPerson = HentPerson(listOf(Navn("fornavn", null, "etternavn")),
                    adressebeskyttelse = listOf(Adressebeskyttelse(adresseGradering))),
            hentIdenter = Identliste(listOf(IdentInformasjon(ident = "987654321")))
    ))
}
