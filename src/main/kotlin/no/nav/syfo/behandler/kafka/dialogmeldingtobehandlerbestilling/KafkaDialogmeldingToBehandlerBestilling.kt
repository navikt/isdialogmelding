package no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling

import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.kafka.kafkaBehandlerDialogmeldingBestillingConsumerConfig
import org.apache.kafka.clients.consumer.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

const val DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka")

fun blockingApplicationLogicDialogmeldingBestilling(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
) {
    val consumerProperties = kafkaBehandlerDialogmeldingBestillingConsumerConfig(applicationEnvironmentKafka)

    val kafkaConsumerDialogmeldingBestilling = KafkaConsumer<String, DialogmeldingToBehandlerBestillingDTO>(consumerProperties)

    kafkaConsumerDialogmeldingBestilling.subscribe(
        listOf(DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC)
    )
    while (applicationState.ready) {
        pollAndProcessDialogmeldingBestilling(
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
            kafkaConsumerDialogmeldingToBehandlerBestilling = kafkaConsumerDialogmeldingBestilling,
        )
    }
}

fun pollAndProcessDialogmeldingBestilling(
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    kafkaConsumerDialogmeldingToBehandlerBestilling: KafkaConsumer<String, DialogmeldingToBehandlerBestillingDTO>,
) {
    val records = kafkaConsumerDialogmeldingToBehandlerBestilling.poll(Duration.ofMillis(1000))
    if (records.count() > 0) {
        createAndStoreDialogmeldingBestillingFromRecords(
            consumerRecords = records,
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        )
        kafkaConsumerDialogmeldingToBehandlerBestilling.commitSync()
    }
}

fun createAndStoreDialogmeldingBestillingFromRecords(
    consumerRecords: ConsumerRecords<String, DialogmeldingToBehandlerBestillingDTO>,
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
) {
    consumerRecords.forEach {
        log.info("Received consumer record with key: ${it.key()}")
        dialogmeldingToBehandlerService.handleIncomingDialogmeldingBestilling(it.value())
    }
}
