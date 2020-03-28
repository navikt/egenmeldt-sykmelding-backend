package no.nav.syfo.sykmelding.kafka.model

private const val SOURCE = "egenmeldt-sykmelding-backend"

data class KafkaMessageMetadata(
    val sykmeldingId: String,
    val source: String = SOURCE
)
