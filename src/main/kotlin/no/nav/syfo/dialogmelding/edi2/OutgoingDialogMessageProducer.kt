package no.nav.syfo.dialogmelding.edi2

import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

const val HELSEMELDING_DIALOG_OUT_TOPIC = "helsemelding.dialog.out"
private const val SOURCE_SYSTEM_HEADER_KEY = "sourceSystem"

class OutgoingDialogMessageProducer(
    private val kafkaProducer: KafkaProducer<String, String>,
    private val sourceSystem: String,
) {
    private val objectMapper = configuredJacksonMapper()

    fun sendDialogmelding(melding: DialogmeldingToBehandlerBestilling) {
        val outgoingDialogMessage = melding.toOutgoingDialogMessage()
        val payload = objectMapper.writeValueAsString(outgoingDialogMessage)
        val key = outgoingDialogMessage.id
        try {
            kafkaProducer.send(
                ProducerRecord(
                    HELSEMELDING_DIALOG_OUT_TOPIC,
                    key,
                    payload,
                ).also {
                    it.headers().add(SOURCE_SYSTEM_HEADER_KEY, sourceSystem.toByteArray())
                }
            ).also { it.get() }
        } catch (e: Exception) {
            log.error("Exception when sending dialogmelding with key $key to $HELSEMELDING_DIALOG_OUT_TOPIC")
            throw e
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutgoingDialogMessageProducer::class.java)
    }
}
