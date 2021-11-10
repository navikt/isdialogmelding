package no.nav.syfo.behandler.kafka

import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Deserializer

val mapper = configuredJacksonMapper()

class JacksonKafkaDeserializer : Deserializer<KafkaBehandlerDialogmeldingDTO> {
    override fun deserialize(topic: String, data: ByteArray): KafkaBehandlerDialogmeldingDTO = mapper.readValue(data, KafkaBehandlerDialogmeldingDTO::class.java)
    override fun close() {}
}
