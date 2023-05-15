package no.nav.syfo.dialogmelding.status.kafka

import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Serializer

class DialogmeldingStatusSerializer : Serializer<KafkaDialogmeldingStatusDTO> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: KafkaDialogmeldingStatusDTO?): ByteArray = mapper.writeValueAsBytes(data)
}
