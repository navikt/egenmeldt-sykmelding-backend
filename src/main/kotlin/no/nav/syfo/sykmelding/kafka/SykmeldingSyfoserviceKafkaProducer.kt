package no.nav.syfo.sykmelding.kafka

import no.nav.helse.sm2013.HelseOpplysningerArbeidsuforhet
import no.nav.syfo.sykmelding.kafka.model.KafkaMessageMetadata
import no.nav.syfo.sykmelding.kafka.model.SykmeldingSyfoserviceKafkaMessage
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class SykmeldingSyfoserviceKafkaProducer(private val kafkaProducer: KafkaProducer<String, SykmeldingSyfoserviceKafkaMessage>, private val topic: String) {
    fun publishSykmeldingToKafka(sykmeldingId: String, helseOpplysningerArbeidsuforhet: HelseOpplysningerArbeidsuforhet) {
        kafkaProducer.send(ProducerRecord(topic, SykmeldingSyfoserviceKafkaMessage(
                metadata = KafkaMessageMetadata(sykmeldingId),
                helseopplysninger = helseOpplysningerArbeidsuforhet
        )))
    }
}
