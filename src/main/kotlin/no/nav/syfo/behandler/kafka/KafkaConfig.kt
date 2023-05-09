package no.nav.syfo.behandler.kafka

import no.nav.syfo.application.kafka.ApplicationEnvironmentKafka
import no.nav.syfo.application.kafka.kafkaConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*

fun kafkaSykmeldingConsumerConfig(
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
): Properties {
    return kafkaConsumerConfig(applicationEnvironmentKafka).apply {
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "500"
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            JacksonKafkaDeserializerSykmelding::class.java.canonicalName
    }
}

fun kafkaDialogmeldingFromBehandlerConsumerConfig(
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
): Properties {
    return kafkaConsumerConfig(applicationEnvironmentKafka).apply {
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            JacksonKafkaDeserializerDialogmeldingFromBehandler::class.java.canonicalName
    }
}
