package no.nav.syfo.sykmelding.service

import no.nav.syfo.log
import no.nav.syfo.model.ReceivedSykmelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class OppdaterTopicsService(
    private val kafkaProducerReceivedSykmelding: KafkaProducer<String, ReceivedSykmelding>,
    private val sm2013AutomaticHandlingTopic: String
) {
    fun oppdaterOKTopic(receivedSykmelding: ReceivedSykmelding) {
        log.info("Skriver sykmelding med id {} til ok-topic", receivedSykmelding.sykmelding.id)
        try {
            kafkaProducerReceivedSykmelding.send(ProducerRecord(sm2013AutomaticHandlingTopic, receivedSykmelding.sykmelding.id, receivedSykmelding)).get()
        } catch (e: Exception) {
            log.error("Noe gikk galt ved skriving til OK-topic: {}", e.cause)
            throw e
        }
    }
}
