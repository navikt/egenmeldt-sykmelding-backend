package no.nav.syfo.sykmelding.mapping

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.eiFellesformat.XMLEIFellesformat
import no.nav.helse.msgHead.XMLCS
import no.nav.helse.msgHead.XMLCV
import no.nav.helse.msgHead.XMLDocument
import no.nav.helse.msgHead.XMLHealthcareProfessional
import no.nav.helse.msgHead.XMLIdent
import no.nav.helse.msgHead.XMLMsgHead
import no.nav.helse.msgHead.XMLMsgInfo
import no.nav.helse.msgHead.XMLOrganisation
import no.nav.helse.msgHead.XMLReceiver
import no.nav.helse.msgHead.XMLRefDoc
import no.nav.helse.msgHead.XMLSender
import no.nav.helse.sm2013.Address
import no.nav.helse.sm2013.ArsakType
import no.nav.helse.sm2013.CS
import no.nav.helse.sm2013.CV
import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.helse.sm2013.Ident
import no.nav.helse.sm2013.NavnType
import no.nav.syfo.log
import no.nav.syfo.sykmelding.model.Pasient

fun mapOcrFilTilFellesformat(
    sykmeldt: Pasient,
    sykmeldingId: String
): XMLEIFellesformat {
    log.info("Mapper sykmelding med id {} til XML-format", sykmeldingId)
    return XMLEIFellesformat().apply {
        any.add(XMLMsgHead().apply {
            msgInfo = XMLMsgInfo().apply {
                type = XMLCS().apply {
                    dn = "Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding"
                    v = "SYKMELD"
                }
                miGversion = "v1.2 2006-05-24"
                genDate = LocalDateTime.now()
                msgId = sykmeldingId
                ack = XMLCS().apply {
                    dn = "Ja"
                    v = "J"
                }
                sender = XMLSender().apply {
                    comMethod = XMLCS().apply {
                        dn = "EDI"
                        v = "EDI"
                    }
                    organisation = XMLOrganisation().apply {
                        healthcareProfessional = XMLHealthcareProfessional().apply {
                            givenName = sykmeldt.fornavn
                            middleName = sykmeldt.mellomnavn
                            familyName = sykmeldt.etternavn
                            ident.addAll(listOf(
                                XMLIdent().apply {
                                    id = "egenmeldt"
                                    typeId = XMLCV().apply {
                                        dn = "HPR-nummer"
                                        s = "6.87.654.3.21.9.8.7.6543.2198"
                                        v = "HPR"
                                    }
                                },
                                XMLIdent().apply {
                                    id = sykmeldt.fnr
                                    typeId = XMLCV().apply {
                                        dn = "Fødselsnummer"
                                        s = "2.16.578.1.12.4.1.1.8327"
                                        v = "FNR"
                                    }
                                }))
                        }
                    }
                }
                receiver = XMLReceiver().apply {
                    comMethod = XMLCS().apply {
                        dn = "EDI"
                        v = "EDI"
                    }
                    organisation = XMLOrganisation().apply {
                        organisationName = "NAV"
                        ident.addAll(listOf(
                            XMLIdent().apply {
                                id = "79768"
                                typeId = XMLCV().apply {
                                    dn = "Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"
                                    s = "2.16.578.1.12.4.1.1.9051"
                                    v = "HER"
                                }
                            },
                            XMLIdent().apply {
                                id = "889640782"
                                typeId = XMLCV().apply {
                                    dn = "Organisasjonsnummeret i Enhetsregister (Brønøysund)"
                                    s = "2.16.578.1.12.4.1.1.9051"
                                    v = "ENH"
                                }
                            }))
                    }
                }
            }
            document.add(XMLDocument().apply {
                refDoc = XMLRefDoc().apply {
                    msgType = XMLCS().apply {
                        dn = "XML-instans"
                        v = "XML"
                    }
                    content = XMLRefDoc.Content().apply {
                        any.add(HelseOpplysningerArbeidsuforhet().apply {
                            syketilfelleStartDato = LocalDate.now()
                            pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                                fodselsnummer = Ident().apply {
                                    id = sykmeldt.fnr
                                    typeId = CV().apply {
                                        dn = "Fødselsnummer"
                                        s = "2.16.578.1.12.4.1.1.8116"
                                        v = "FNR"
                                    }
                                }
                            }
                            arbeidsgiver = tilArbeidsgiver()
                            medisinskVurdering = tilMedisinskVurdering()
                            aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                                periode.addAll(tilPeriodeListe())
                            }
                            kontaktMedPasient = HelseOpplysningerArbeidsuforhet.KontaktMedPasient().apply {
                                kontaktDato = LocalDate.now()
                                begrunnIkkeKontakt = null
                                behandletDato = LocalDateTime.now()
                            }
                            behandler = tilBehandler(sykmeldt)
                            avsenderSystem = HelseOpplysningerArbeidsuforhet.AvsenderSystem().apply {
                                systemNavn = "Egenmeldt"
                                systemVersjon = "1"
                            }
                        })
                    }
                }
            })
        })
    }
}

fun tilBehandler(sykmeldt: Pasient): HelseOpplysningerArbeidsuforhet.Behandler =
    HelseOpplysningerArbeidsuforhet.Behandler().apply {
        navn = NavnType().apply {
            fornavn = sykmeldt.fornavn ?: ""
            mellomnavn = sykmeldt.mellomnavn
            etternavn = sykmeldt.etternavn ?: ""
        }
        id.addAll(listOf(
            Ident().apply {
                id = "egenmeldt"
                typeId = CV().apply {
                    dn = "HPR-nummer"
                    s = "6.87.654.3.21.9.8.7.6543.2198"
                    v = "HPR"
                }
            },
            Ident().apply {
                id = sykmeldt.fnr
                typeId = CV().apply {
                    dn = "Fødselsnummer"
                    s = "2.16.578.1.12.4.1.1.8327"
                    v = "FNR"
                }
            }))
        adresse = Address()
    }

fun tilPeriodeListe(): List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode> {
    val periodeListe = ArrayList<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>()
    periodeListe.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
        periodeFOMDato = LocalDate.now()
        periodeTOMDato = LocalDate.now().plusDays(10)
        aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
            medisinskeArsaker = ArsakType().apply {
                beskriv = "Har korona"
                arsakskode.add(CS())
            }
            arbeidsplassen = null
        }
        avventendeSykmelding = null
        gradertSykmelding = null
        behandlingsdager = null
        isReisetilskudd = false
    })
    return periodeListe
}

fun tilArbeidsgiver(): HelseOpplysningerArbeidsuforhet.Arbeidsgiver =
    HelseOpplysningerArbeidsuforhet.Arbeidsgiver().apply {
        harArbeidsgiver = CS().apply {
            dn = "Én arbeidsgiver"
            v = "1"
        }
        navnArbeidsgiver = "Bedriften AS"
        yrkesbetegnelse = "Utvikler"
        stillingsprosent = 100
    }

fun tilMedisinskVurdering(): HelseOpplysningerArbeidsuforhet.MedisinskVurdering {
    return HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
        hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
            diagnosekode = CV().apply {
                s = "R991"
                v = "R991"
                dn = "Mistenkt covid-19"
            }
        }
        isSkjermesForPasient = false
        isSvangerskap = false
        isYrkesskade = false
        yrkesskadeDato = null
    }
}
