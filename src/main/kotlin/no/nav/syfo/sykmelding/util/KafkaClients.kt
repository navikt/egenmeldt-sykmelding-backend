package no.nav.syfo.sykmelding.util

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.sykmelding.kafka.SykmeldingSyfoserviceKafkaProducer
import no.nav.syfo.sykmelding.kafka.model.SykmeldingSyfoserviceKafkaMessage
import org.apache.kafka.clients.producer.KafkaProducer

class KafkaClients(env: Environment, vaultSecrets: VaultSecrets) {

    private val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
    private val producerProperties = kafkaBaseConfig.toProducerConfig(env.applicationName, valueSerializer = JacksonKafkaSerializer::class)

    val kafkaProducerReceivedSykmelding = KafkaProducer<String, ReceivedSykmelding>(producerProperties)
    val syfoserviceKafkaProducer = SykmeldingSyfoserviceKafkaProducer(KafkaProducer<String, SykmeldingSyfoserviceKafkaMessage>(producerProperties), env.syfoserviceKafkaTopic)
}
