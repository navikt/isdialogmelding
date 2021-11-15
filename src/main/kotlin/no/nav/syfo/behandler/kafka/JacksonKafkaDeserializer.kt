package no.nav.syfo.behandler.kafka

import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Deserializer

val mapper = configuredJacksonMapper()

class JacksonKafkaDeserializer : Deserializer<BehandlerDialogmeldingBestillingDTO> {
    override fun deserialize(topic: String, data: ByteArray): BehandlerDialogmeldingBestillingDTO = mapper.readValue(data, BehandlerDialogmeldingBestillingDTO::class.java)
    override fun close() {}
}
