package no.nav.syfo.dialogmelding.edi2.status

import no.nav.syfo.application.kafka.ApplicationEnvironmentKafka
import no.nav.syfo.application.kafka.kafkaConsumerConfig
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.Properties

fun kafkaDialogmeldingStatusFromEdi2ConsumerConfig(
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
): Properties {
    return kafkaConsumerConfig(applicationEnvironmentKafka).apply {
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            JacksonKafkaDeserializerDialogmeldingStatusFromEdi2::class.java.canonicalName
    }
}

class JacksonKafkaDeserializerDialogmeldingStatusFromEdi2 : Deserializer<KafkaDialogmeldingStatusFromEdi2DTO> {
    private val mapper = configuredJacksonMapper()

    override fun deserialize(topic: String, data: ByteArray): KafkaDialogmeldingStatusFromEdi2DTO =
        mapper.readValue(data, KafkaDialogmeldingStatusFromEdi2DTO::class.java)
}
