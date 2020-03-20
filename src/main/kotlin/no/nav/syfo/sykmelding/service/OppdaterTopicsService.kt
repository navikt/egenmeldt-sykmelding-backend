package no.nav.syfo.sykmelding.service

import no.nav.syfo.log
import no.nav.syfo.model.ReceivedSykmelding
import no.nav.syfo.model.Status
import no.nav.syfo.model.ValidationResult
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class OppdaterTopicsService(
    private val kafkaProducerReceivedSykmelding: KafkaProducer<String, ReceivedSykmelding>,
    private val kafkaProducerValidationResult: KafkaProducer<String, ValidationResult>,
    private val sm2013AutomaticHandlingTopic: String,
    private val sm2013BehandlingsUtfallTopic: String
) {
    fun oppdaterTopics(receivedSykmelding: ReceivedSykmelding) {
        log.info("Skriver sykmelding med id {} til ok-topic", receivedSykmelding.sykmelding.id)
        kafkaProducerReceivedSykmelding.send(ProducerRecord(sm2013AutomaticHandlingTopic, receivedSykmelding.sykmelding.id, receivedSykmelding))

        val validationResult = ValidationResult(Status.OK, emptyList())
        log.info("Skriver behandlingsutfall for sykmelding med id {} til ok-topic", receivedSykmelding.sykmelding.id)
        kafkaProducerValidationResult.send(ProducerRecord(sm2013BehandlingsUtfallTopic, receivedSykmelding.sykmelding.id, validationResult))
    }
}
