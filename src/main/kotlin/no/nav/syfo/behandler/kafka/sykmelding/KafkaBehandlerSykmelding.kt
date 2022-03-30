package no.nav.syfo.behandler.kafka.sykmelding

import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.behandler.kafka.kafkaSykmeldingConsumerConfig
import org.apache.kafka.clients.consumer.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

const val SYKMELDING_TOPIC = "teamsykmelding.ok-sykmelding"

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka")

fun blockingApplicationLogicSykmelding(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
) {
    val consumerProperties = kafkaSykmeldingConsumerConfig(applicationEnvironmentKafka)

    val kafkaConsumerSykmelding = KafkaConsumer<String, ReceivedSykmeldingDTO>(consumerProperties)

    kafkaConsumerSykmelding.subscribe(
        listOf(SYKMELDING_TOPIC)
    )
    while (applicationState.ready) {
        pollAndProcessSykmelding(
            kafkaConsumerSykmelding = kafkaConsumerSykmelding,
        )
    }
}

fun pollAndProcessSykmelding(
    kafkaConsumerSykmelding: KafkaConsumer<String, ReceivedSykmeldingDTO>,
) {
    val records = kafkaConsumerSykmelding.poll(Duration.ofMillis(1000))
    if (records.count() > 0) {
        createAndStoreBehandlerFromSykmelding(
            consumerRecords = records,
        )
        kafkaConsumerSykmelding.commitSync()
    }
}

fun createAndStoreBehandlerFromSykmelding(
    consumerRecords: ConsumerRecords<String, ReceivedSykmeldingDTO>,
) {
    consumerRecords.forEach {
        log.info("Received sykmelding record with key: ${it.key()}")
    }
}
