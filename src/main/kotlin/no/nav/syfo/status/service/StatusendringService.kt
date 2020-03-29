package no.nav.syfo.status.service

import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.log
import no.nav.syfo.model.sykmeldingstatus.StatusEventDTO
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.syfo.sykmelding.db.slettEgenmeldtSykmelding

class StatusendringService(private val database: DatabaseInterface) {

    fun handterStatusendring(sykmeldingStatusKafkaMessageDTO: SykmeldingStatusKafkaMessageDTO) {
        if (sykmeldingStatusKafkaMessageDTO.event.statusEvent == StatusEventDTO.AVBRUTT) {
            val sykmeldingId = sykmeldingStatusKafkaMessageDTO.kafkaMetadata.sykmeldingId
            log.info("Har mottatt statusendring for avbrutt sykmelding med id {}", sykmeldingId)
            database.slettEgenmeldtSykmelding(UUID.fromString(sykmeldingId))
        }
    }
}
