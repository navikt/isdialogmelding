package no.nav.syfo.dialogmelding.status.kafka

import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus
import no.nav.syfo.dialogmelding.status.domain.toKafkaDialogmeldingStatusDTO
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class DialogmeldingStatusProducer(
    private val kafkaProducer: KafkaProducer<String, KafkaDialogmeldingStatusDTO>,
) {
    fun sendDialogmeldingStatus(status: DialogmeldingStatus) {
        val kafkaDialogmeldingStatusDTO = status.toKafkaDialogmeldingStatusDTO()
        val key = status.bestilling.uuid
        try {
            kafkaProducer.send(
                ProducerRecord(
                    BEHANDLER_DIALOGMELDING_STATUS_TOPIC,
                    key.toString(),
                    kafkaDialogmeldingStatusDTO,
                ),
            ).get()
        } catch (e: Exception) {
            log.error(
                "Exception was thrown when attempting to send DialogmeldingStatusDTO with key $key to kafka: ${e.message}",
            )
            throw e
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmeldingStatusProducer::class.java)
        const val BEHANDLER_DIALOGMELDING_STATUS_TOPIC = "teamsykefravr.behandler-dialogmelding-status"
    }
}
