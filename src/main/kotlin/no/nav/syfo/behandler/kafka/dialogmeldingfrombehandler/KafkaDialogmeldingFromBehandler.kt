package no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler

import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.behandler.kafka.kafkaDialogmeldingFromBehandlerConsumerConfig
import org.apache.kafka.clients.consumer.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

const val DIALOGMELDING_FROM_BEHANDLER_TOPIC = "teamsykefravr.dialogmelding"

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka.dialogmeldingFromBehandler")

fun blockingApplicationLogicDialogmeldingFromBehandler(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
) {
    val consumerProperties = kafkaDialogmeldingFromBehandlerConsumerConfig(applicationEnvironmentKafka)
    val kafkaConsumerDialogmelding = KafkaConsumer<String, KafkaDialogmeldingFromBehandlerDTO>(consumerProperties)
    kafkaConsumerDialogmelding.subscribe(
        listOf(DIALOGMELDING_FROM_BEHANDLER_TOPIC)
    )
    while (applicationState.ready) {
        pollAndProcessDialogmeldingFromBehandler(
            kafkaConsumerDialogmeldingFromBehandler = kafkaConsumerDialogmelding,
        )
    }
}

fun pollAndProcessDialogmeldingFromBehandler(
    kafkaConsumerDialogmeldingFromBehandler: KafkaConsumer<String, KafkaDialogmeldingFromBehandlerDTO>,
) {
    val records = kafkaConsumerDialogmeldingFromBehandler.poll(Duration.ofMillis(1000))
    if (records.count() > 0) {
        updateBehandlerOffice(
            consumerRecords = records,
        )
        kafkaConsumerDialogmeldingFromBehandler.commitSync()
    }
}

// TODO Hva skal denne faktisk gjøre?
fun updateBehandlerOffice(
    consumerRecords: ConsumerRecords<String, KafkaDialogmeldingFromBehandlerDTO>,
) {
    consumerRecords.forEach {
        log.info("Received dialogmelding record with key: ${it.key()}")
    }
}
