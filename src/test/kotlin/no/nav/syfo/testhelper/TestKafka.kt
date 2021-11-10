package no.nav.syfo.testhelper

import no.nav.common.KafkaEnvironment
import no.nav.syfo.behandler.kafka.DIALOGMELDING_BESTILLING_TOPIC

fun testKafka(
    autoStart: Boolean = false,
    withSchemaRegistry: Boolean = false,
    topicNames: List<String> = listOf(
        DIALOGMELDING_BESTILLING_TOPIC,
    )
) = KafkaEnvironment(
    autoStart = autoStart,
    withSchemaRegistry = withSchemaRegistry,
    topicNames = topicNames,
)
