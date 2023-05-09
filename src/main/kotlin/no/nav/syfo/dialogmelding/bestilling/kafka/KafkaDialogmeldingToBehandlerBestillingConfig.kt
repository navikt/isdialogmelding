package no.nav.syfo.dialogmelding.bestilling.kafka

import no.nav.syfo.application.kafka.*
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

fun kafkaBehandlerDialogmeldingBestillingConsumerConfig(
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
): Properties {
    return kafkaConsumerConfig(applicationEnvironmentKafka).apply {
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            JacksonKafkaDeserializerBehandlerDialogmeldingBestilling::class.java.canonicalName
    }
}

class JacksonKafkaDeserializerBehandlerDialogmeldingBestilling : Deserializer<DialogmeldingToBehandlerBestillingDTO> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): DialogmeldingToBehandlerBestillingDTO =
        mapper.readValue(data, DialogmeldingToBehandlerBestillingDTO::class.java)
}
