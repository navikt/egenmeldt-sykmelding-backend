package no.nav.syfo.pdl.service

import io.mockk.coEvery
import io.mockk.mockkClass
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.OidcToken
import no.nav.syfo.client.StsOidcClient
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.client.model.Adressebeskyttelse
import no.nav.syfo.pdl.client.model.GetPersonResponse
import no.nav.syfo.pdl.client.model.HentPerson
import no.nav.syfo.pdl.client.model.Navn
import no.nav.syfo.pdl.client.model.ResponseData
import no.nav.syfo.pdl.error.PersonNotFoundInPdl
import no.nav.syfo.pdl.service.PdlPersonService.Companion.STRENGT_FORTROLIG
import no.nav.syfo.pdl.service.PdlPersonService.Companion.STRENGT_FORTROLIG_UTLAND
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class PdlServiceTest : Spek({
    val pdlClient = mockkClass(PdlClient::class)
    val stsOidcClient = mockkClass(StsOidcClient::class)
    val pdlService = PdlPersonService(pdlClient, stsOidcClient)
    coEvery { stsOidcClient.oidcToken() } returns OidcToken("Token", "JWT", 1L)
    describe("PdlService") {
        it("hente person fra pdl uten fortrolig dresse") {
            coEvery { pdlClient.getPerson(any(), any(), any()) } returns getPdlResponse("UGRADERT")
            runBlocking {
                val person = pdlService.getPersonOgDiskresjonskode("01245678901", "Bearer token")
                person.fortroligAdresse shouldEqual false
                person.navn.fornavn shouldEqual "fornavn"
                person.navn.mellomnavn shouldEqual null
                person.navn.etternavn shouldEqual "etternavn"
            }
        }
        it("hente person fra pdl fortrolig dresse") {
            coEvery { pdlClient.getPerson(any(), any(), any()) } returns getPdlResponse("FORTROLIG")
            runBlocking {
                val person = pdlService.getPersonOgDiskresjonskode("01245678901", "Bearer token")
                person.fortroligAdresse shouldEqual false
            }
        }
        it("hente person fra pdl strengt fortrolig dresse") {
            coEvery { pdlClient.getPerson(any(), any(), any()) } returns getPdlResponse(STRENGT_FORTROLIG)
            runBlocking {
                val person = pdlService.getPersonOgDiskresjonskode("01245678901", "Bearer token")
                person.fortroligAdresse shouldEqual true
            }
        }
        it("hente person fra pdl strengt fortrolig dresse") {
            coEvery { pdlClient.getPerson(any(), any(), any()) } returns getPdlResponse(STRENGT_FORTROLIG_UTLAND)
            runBlocking {
                val person = pdlService.getPersonOgDiskresjonskode("01245678901", "Bearer token")
                person.fortroligAdresse shouldEqual true
            }
        }

        it("Skal feile når person ikke finnes") {
            coEvery { pdlClient.getPerson(any(), any(), any()) } returns GetPersonResponse(ResponseData(null))
            val exception = assertFailsWith<PersonNotFoundInPdl> {
                runBlocking {
                    pdlService.getPersonOgDiskresjonskode("123", "Bearer token")
                }
            }
            exception.message shouldEqual "Fant ikke person i PDL"
        }

        it("Skal feile når navn er tom liste") {
            coEvery { pdlClient.getPerson(any(), any(), any()) } returns GetPersonResponse(ResponseData(HentPerson(
                    navn = emptyList(),
                    adressebeskyttelse = listOf(Adressebeskyttelse("UGRADERT"))
            )))
            val exception = assertFailsWith<PersonNotFoundInPdl> {
                runBlocking {
                    pdlService.getPersonOgDiskresjonskode("123", "Bearer token")
                }
            }
            exception.message shouldEqual "Fant ikke navn på person i PDL"
        }
        it("Skal feile når navn ikke finnes") {
            coEvery { pdlClient.getPerson(any(), any(), any()) } returns GetPersonResponse(ResponseData(HentPerson(
                    navn = null,
                    adressebeskyttelse = listOf(Adressebeskyttelse("UGRADERT"))
            )))
            val exception = assertFailsWith<PersonNotFoundInPdl> {
                runBlocking {
                    pdlService.getPersonOgDiskresjonskode("123", "Bearer token")
                }
            }
            exception.message shouldEqual "Fant ikke navn på person i PDL"
        }

        it("Skal feile når gradering ikke finnes") {
            coEvery { pdlClient.getPerson(any(), any(), any()) } returns GetPersonResponse(ResponseData(HentPerson(
                    navn = listOf(Navn("fornavn", "mellomnavn", "etternanv")),
                    adressebeskyttelse = emptyList())
            ))
            val exception = assertFailsWith<PersonNotFoundInPdl> {
                runBlocking {
                    pdlService.getPersonOgDiskresjonskode("123", "Bearer token")
                }
            }
            exception.message shouldEqual "Fant ikke diskresjonskode i PDL"
        }
    }
})
