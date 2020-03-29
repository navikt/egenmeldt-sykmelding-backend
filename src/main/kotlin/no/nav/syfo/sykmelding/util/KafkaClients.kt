package no.nav.syfo.sykmelding.util

import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.kafka.envOverrides
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.syfo.sykmelding.kafka.SykmeldingSyfoserviceKafkaProducer
import no.nav.syfo.sykmelding.kafka.model.SykmeldingSyfoserviceKafkaMessage
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer

class KafkaClients(env: Environment, vaultSecrets: VaultSecrets) {

    private val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
    private val producerProperties = kafkaBaseConfig.toProducerConfig(env.applicationName, valueSerializer = JacksonKafkaSerializer::class)

    val kafkaProducerReceivedSykmelding = KafkaProducer<String, ReceivedSykmelding>(producerProperties)
    val syfoserviceKafkaProducer = SykmeldingSyfoserviceKafkaProducer(KafkaProducer<String, SykmeldingSyfoserviceKafkaMessage>(producerProperties), env.syfoserviceKafkaTopic)
    val kafkaStatusConsumer = getKafkaStatusConsumer(vaultSecrets, env)

    private fun getKafkaStatusConsumer(vaultSecrets: VaultSecrets, env: Environment): KafkaConsumer<String, SykmeldingStatusKafkaMessageDTO> {
        val kafkaBaseConfigForStatus = loadBaseConfig(env, vaultSecrets).envOverrides()
        kafkaBaseConfigForStatus["auto.offset.reset"] = "latest"
        val properties = kafkaBaseConfigForStatus.toConsumerConfig("${env.applicationName}-consumer", JacksonKafkaDeserializer::class)
        properties.let { it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1" }
        val kafkaStatusConsumer = KafkaConsumer<String, SykmeldingStatusKafkaMessageDTO>(properties, StringDeserializer(), JacksonKafkaDeserializer(SykmeldingStatusKafkaMessageDTO::class))
        kafkaStatusConsumer.subscribe(listOf(env.sykmeldingStatusTopic))
        return kafkaStatusConsumer
    }
}
