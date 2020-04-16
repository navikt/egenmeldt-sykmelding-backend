package no.nav.syfo.status.service

import java.util.UUID
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.model.sykmeldingstatus.StatusEventDTO
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.syfo.sykmelding.db.slettEgenmeldtSykmelding

class StatusendringService(private val database: DatabaseInterface) {

    fun handterStatusendring(sykmeldingStatusKafkaMessageDTO: SykmeldingStatusKafkaMessageDTO) {
        if (sykmeldingStatusKafkaMessageDTO.event.statusEvent == StatusEventDTO.AVBRUTT) {
            val sykmeldingId = sykmeldingStatusKafkaMessageDTO.kafkaMetadata.sykmeldingId
            database.slettEgenmeldtSykmelding(UUID.fromString(sykmeldingId))
        }
    }
}
