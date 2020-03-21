package no.nav.syfo.sykmelding.util

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.model.ReceivedSykmelding
import org.apache.kafka.clients.producer.KafkaProducer

class KafkaClients(env: Environment, vaultSecrets: VaultSecrets) {

    private val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
    private val producerProperties = kafkaBaseConfig.toProducerConfig(env.applicationName, valueSerializer = JacksonKafkaSerializer::class)

    val kafkaProducerReceivedSykmelding = KafkaProducer<String, ReceivedSykmelding>(producerProperties)
}
