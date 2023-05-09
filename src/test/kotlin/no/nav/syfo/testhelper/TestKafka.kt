package no.nav.syfo.testhelper

import no.nav.common.KafkaEnvironment
import no.nav.syfo.dialogmelding.bestilling.kafka.DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC
import no.nav.syfo.identhendelse.kafka.PDL_AKTOR_TOPIC

fun testKafka(
    autoStart: Boolean = false,
    withSchemaRegistry: Boolean = false,
    topicNames: List<String> = listOf(
        DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
        PDL_AKTOR_TOPIC,
    )
) = KafkaEnvironment(
    autoStart = autoStart,
    withSchemaRegistry = withSchemaRegistry,
    topicNames = topicNames,
)
