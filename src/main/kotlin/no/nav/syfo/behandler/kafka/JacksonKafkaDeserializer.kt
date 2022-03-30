package no.nav.syfo.behandler.kafka

import no.nav.syfo.behandler.kafka.behandlerdialogmelding.BehandlerDialogmeldingBestillingDTO
import no.nav.syfo.behandler.kafka.sykmelding.ReceivedSykmeldingDTO
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Deserializer

val mapper = configuredJacksonMapper()

class JacksonKafkaDeserializerBehandlerDialogmeldingBestilling : Deserializer<BehandlerDialogmeldingBestillingDTO> {
    override fun deserialize(topic: String, data: ByteArray): BehandlerDialogmeldingBestillingDTO =
        mapper.readValue(data, BehandlerDialogmeldingBestillingDTO::class.java)
    override fun close() {}
}

class JacksonKafkaDeserializerSykmelding : Deserializer<ReceivedSykmeldingDTO> {
    override fun deserialize(topic: String, data: ByteArray): ReceivedSykmeldingDTO =
        mapper.readValue(data, ReceivedSykmeldingDTO::class.java)
    override fun close() {}
}
