package no.nav.syfo.sykmelding.mapping

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.RuntimeException
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
import no.nav.helse.sm2013.TeleCom
import no.nav.helse.sm2013.URL
import no.nav.syfo.log
import no.nav.syfo.sykmelding.model.Arbeidsforhold
import no.nav.syfo.sykmelding.model.Pasient

fun opprettFellesformat(
    sykmeldt: Pasient,
    sykmeldingId: String,
    fom: LocalDate,
    tom: LocalDate,
    arbeidsforhold: Arbeidsforhold?,
    antallArbeidsgivere: Int
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
                            syketilfelleStartDato = fom
                            pasient = HelseOpplysningerArbeidsuforhet.Pasient().apply {
                                navn = NavnType().apply {
                                    fornavn = sykmeldt.fornavn
                                    mellomnavn = sykmeldt.mellomnavn
                                    etternavn = sykmeldt.etternavn
                                }
                                fodselsnummer = Ident().apply {
                                    id = sykmeldt.fnr
                                    typeId = CV().apply {
                                        dn = "Fødselsnummer"
                                        s = "2.16.578.1.12.4.1.1.8116"
                                        v = "FNR"
                                    }
                                }
                            }
                            arbeidsgiver = tilArbeidsgiver(arbeidsforhold, antallArbeidsgivere)
                            medisinskVurdering = tilMedisinskVurdering()
                            aktivitet = HelseOpplysningerArbeidsuforhet.Aktivitet().apply {
                                periode.addAll(tilPeriodeListe(fom = fom, tom = tom))
                            }
                            prognose = null
                            utdypendeOpplysninger = null
                            tiltak = null
                            meldingTilNav = null
                            meldingTilArbeidsgiver = null
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
                            strekkode = "123456789qwerty"
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
        kontaktInfo.add(TeleCom().apply {
            typeTelecom = CS().apply {
                v = "HP"
                dn = "Hovedtelefon"
            }
            teleAddress = URL().apply {
                v = "tel:55553336"
            }
        })
    }

fun tilPeriodeListe(fom: LocalDate, tom: LocalDate): List<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode> {
    val periodeListe = ArrayList<HelseOpplysningerArbeidsuforhet.Aktivitet.Periode>()
    periodeListe.add(HelseOpplysningerArbeidsuforhet.Aktivitet.Periode().apply {
        periodeFOMDato = fom
        periodeTOMDato = tom
        aktivitetIkkeMulig = HelseOpplysningerArbeidsuforhet.Aktivitet.Periode.AktivitetIkkeMulig().apply {
            medisinskeArsaker = ArsakType().apply {
                beskriv = null
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

fun tilArbeidsgiver(arbeidsforhold: Arbeidsforhold?, antallArbeidsgivere: Int): HelseOpplysningerArbeidsuforhet.Arbeidsgiver =
    HelseOpplysningerArbeidsuforhet.Arbeidsgiver().apply {
        harArbeidsgiver =
            when {
                antallArbeidsgivere == 1 -> CS().apply {
                    dn = "Én arbeidsgiver"
                    v = "1"
                }
                antallArbeidsgivere > 1 -> CS().apply {
                    dn = "Flere arbeidsgivere"
                    v = "2"
                }
                antallArbeidsgivere == 0 -> CS().apply {
                    dn = "Ingen arbeidsgiver"
                    v = "3"
                }
                else -> {
                    log.error("Antall arbeidsforhold er mindre enn 0, skal ikke kunne skje")
                    throw RuntimeException("Antall arbeidsforhold er mindre enn 0, skal ikke kunne skje")
                }
            }
        navnArbeidsgiver = arbeidsforhold?.navn
        yrkesbetegnelse = ""
        stillingsprosent = arbeidsforhold?.stillingsprosent?.toInt()
    }

fun tilMedisinskVurdering(): HelseOpplysningerArbeidsuforhet.MedisinskVurdering {
    return HelseOpplysningerArbeidsuforhet.MedisinskVurdering().apply {
        hovedDiagnose = HelseOpplysningerArbeidsuforhet.MedisinskVurdering.HovedDiagnose().apply {
            diagnosekode = CV().apply {
                s = "2.16.578.1.12.4.1.1.7170"
                v = "R991"
                dn = "COVID-19 (MISTENKT ELLER BEKREFTET)"
            }
        }
        isSkjermesForPasient = false
        annenFraversArsak = null
        isSvangerskap = false
        isYrkesskade = false
        yrkesskadeDato = null
    }
}
