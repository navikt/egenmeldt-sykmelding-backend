package no.nav.syfo.syfosmregister.client

fun getTestResponse(): String = "[\n" +
        "    {\n" +
        "        \"id\": \"1\",\n" +
        "        \"mottattTidspunkt\": \"2020-03-24T07:59:09.181101Z\",\n" +
        "        \"behandlingsutfall\": {\n" +
        "            \"status\": \"OK\",\n" +
        "            \"ruleHits\": []\n" +
        "        },\n" +
        "        \"legekontorOrgnummer\": null,\n" +
        "        \"arbeidsgiver\": null,\n" +
        "        \"sykmeldingsperioder\": [\n" +
        "            {\n" +
        "                \"fom\": \"2020-03-24\",\n" +
        "                \"tom\": \"2020-03-24\",\n" +
        "                \"gradert\": null,\n" +
        "                \"behandlingsdager\": null,\n" +
        "                \"innspillTilArbeidsgiver\": null,\n" +
        "                \"type\": \"AKTIVITET_IKKE_MULIG\",\n" +
        "                \"aktivitetIkkeMulig\": null,\n" +
        "                \"reisetilskudd\": false\n" +
        "            }\n" +
        "        ],\n" +
        "        \"sykmeldingStatus\": {\n" +
        "            \"statusEvent\": \"APEN\",\n" +
        "            \"timestamp\": \"2020-03-24T07:59:09.178026Z\",\n" +
        "            \"arbeidsgiver\": null,\n" +
        "            \"sporsmalOgSvarListe\": []\n" +
        "        },\n" +
        "        \"medisinskVurdering\": {\n" +
        "            \"hovedDiagnose\": {\n" +
        "                \"kode\": \"1\",\n" +
        "                \"system\": \"system\",\n" +
        "                \"tekst\": \"hoveddiagnose\"\n" +
        "            },\n" +
        "            \"biDiagnoser\": [\n" +
        "                {\n" +
        "                    \"kode\": \"2\",\n" +
        "                    \"system\": \"system2\",\n" +
        "                    \"tekst\": \"bidagnose\"\n" +
        "                }\n" +
        "            ],\n" +
        "            \"annenFraversArsak\": {\n" +
        "                \"beskrivelse\": \"\",\n" +
        "                \"grunn\": []\n" +
        "            },\n" +
        "            \"svangerskap\": false,\n" +
        "            \"yrkesskade\": false,\n" +
        "            \"yrkesskadeDato\": null\n" +
        "        },\n" +
        "        \"skjermesForPasient\": false,\n" +
        "        \"prognose\": null,\n" +
        "        \"utdypendeOpplysninger\": {},\n" +
        "        \"tiltakArbeidsplassen\": null,\n" +
        "        \"tiltakNAV\": null,\n" +
        "        \"andreTiltak\": null,\n" +
        "        \"meldingTilNAV\": null,\n" +
        "        \"meldingTilArbeidsgiver\": null,\n" +
        "        \"kontaktMedPasient\": {\n" +
        "            \"kontaktDato\": null,\n" +
        "            \"begrunnelseIkkeKontakt\": null\n" +
        "        },\n" +
        "        \"behandletTidspunkt\": \"2020-03-24T07:59:09.181085Z\",\n" +
        "        \"behandler\": {\n" +
        "            \"fornavn\": \"fornavn\",\n" +
        "            \"mellomnavn\": null,\n" +
        "            \"etternavn\": \"etternavn\",\n" +
        "            \"aktoerId\": \"123\",\n" +
        "            \"fnr\": \"01234567891\",\n" +
        "            \"hpr\": null,\n" +
        "            \"her\": null,\n" +
        "            \"adresse\": {\n" +
        "                \"gate\": null,\n" +
        "                \"postnummer\": null,\n" +
        "                \"kommune\": null,\n" +
        "                \"postboks\": null,\n" +
        "                \"land\": null\n" +
        "            },\n" +
        "            \"tlf\": null\n" +
        "        },\n" +
        "        \"syketilfelleStartDato\": null,\n" +
        "        \"navnFastlege\": null,\n" +
        "        \"egenmeldt\": false,\n" +
        "        \"harRedusertArbeidsgiverperiode\": false\n" +
        "    }\n" +
        "]"
