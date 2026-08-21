package no.nav.syfo.behandler.kafka

import no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler.KafkaDialogmeldingFromBehandlerDTO
import no.nav.syfo.behandler.kafka.sykmelding.ReceivedSykmeldingDTO
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Deserializer

val mapper = configuredJacksonMapper()

class JacksonKafkaDeserializerSykmelding : Deserializer<ReceivedSykmeldingDTO> {
    override fun deserialize(topic: String, data: ByteArray): ReceivedSykmeldingDTO =
        mapper.readValue(data, ReceivedSykmeldingDTO::class.java)
    override fun close() {}
}

class JacksonKafkaDeserializerDialogmeldingFromBehandler : Deserializer<KafkaDialogmeldingFromBehandlerDTO> {
    override fun deserialize(topic: String, data: ByteArray): KafkaDialogmeldingFromBehandlerDTO =
        mapper.readValue(data, KafkaDialogmeldingFromBehandlerDTO::class.java)
    override fun close() {}
}
