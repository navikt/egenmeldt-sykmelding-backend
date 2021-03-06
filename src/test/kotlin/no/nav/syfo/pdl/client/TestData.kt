package no.nav.syfo.pdl.client

fun getUgradertTestData(): String {
    return "{\n" +
            "  \"data\": {\n" +
            "    \"hentIdenter\": {\n" +
            "      \"identer\": [\n" +
            "        {\n" +
            "          \"ident\": \"987654321\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"hentPerson\": {\n" +
            "      \"navn\": [\n" +
            "        {\n" +
            "          \"fornavn\": \"RASK\",\n" +
            "          \"mellomnavn\": null,\n" +
            "          \"etternavn\": \"SAKS\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"adressebeskyttelse\": [\n" +
            "        {\n" +
            "          \"gradering\": \"UGRADERT\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}"
}

fun getStrengtFortroligTestData(): String {
    return "{\n" +
            "  \"data\": {\n" +
            "    \"hentIdenter\": {\n" +
            "      \"identer\": [\n" +
            "        {\n" +
            "          \"ident\": \"987654321\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"hentPerson\": {\n" +
            "      \"navn\": [\n" +
            "        {\n" +
            "          \"fornavn\": \"RASK\",\n" +
            "          \"mellomnavn\": null,\n" +
            "          \"etternavn\": \"SAKS\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"adressebeskyttelse\": [\n" +
            "        {\n" +
            "          \"gradering\": \"STRENGT_FORTROLIG\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}"
}

fun getErrorResponse(): String {
    return "{\n" +
            "  \"errors\": [\n" +
            "    {\n" +
            "      \"message\": \"Ikke tilgang til å se person\",\n" +
            "      \"locations\": [\n" +
            "        {\n" +
            "          \"line\": 2,\n" +
            "          \"column\": 3\n" +
            "        }\n" +
            "      ],\n" +
            "      \"path\": [\n" +
            "        \"hentPerson\"\n" +
            "      ],\n" +
            "      \"extensions\": {\n" +
            "        \"code\": \"unauthorized\",\n" +
            "        \"classification\": \"ExecutionAborted\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"data\": {\n" +
            "    \"hentPerson\": null\n" +
            "  }\n" +
            "}"
}
