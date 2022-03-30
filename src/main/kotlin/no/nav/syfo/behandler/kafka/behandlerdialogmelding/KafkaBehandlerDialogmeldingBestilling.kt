package no.nav.syfo.behandler.kafka.behandlerdialogmelding

import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.behandler.kafka.kafkaBehandlerDialogmeldingBestillingConsumerConfig
import org.apache.kafka.clients.consumer.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

const val DIALOGMELDING_BESTILLING_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka")

fun blockingApplicationLogicDialogmeldingBestilling(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
    behandlerDialogmeldingService: BehandlerDialogmeldingService,
) {
    val consumerProperties = kafkaBehandlerDialogmeldingBestillingConsumerConfig(applicationEnvironmentKafka)

    val kafkaConsumerDialogmeldingBestilling = KafkaConsumer<String, BehandlerDialogmeldingBestillingDTO>(consumerProperties)

    kafkaConsumerDialogmeldingBestilling.subscribe(
        listOf(DIALOGMELDING_BESTILLING_TOPIC)
    )
    while (applicationState.ready) {
        pollAndProcessDialogmeldingBestilling(
            behandlerDialogmeldingService = behandlerDialogmeldingService,
            kafkaConsumerDialogmeldingBestilling = kafkaConsumerDialogmeldingBestilling,
        )
    }
}

fun pollAndProcessDialogmeldingBestilling(
    behandlerDialogmeldingService: BehandlerDialogmeldingService,
    kafkaConsumerDialogmeldingBestilling: KafkaConsumer<String, BehandlerDialogmeldingBestillingDTO>,
) {
    val records = kafkaConsumerDialogmeldingBestilling.poll(Duration.ofMillis(1000))
    if (records.count() > 0) {
        createAndStoreDialogmeldingBestillingFromRecords(
            consumerRecords = records,
            behandlerDialogmeldingService = behandlerDialogmeldingService,
        )
        kafkaConsumerDialogmeldingBestilling.commitSync()
    }
}

fun createAndStoreDialogmeldingBestillingFromRecords(
    consumerRecords: ConsumerRecords<String, BehandlerDialogmeldingBestillingDTO>,
    behandlerDialogmeldingService: BehandlerDialogmeldingService,
) {
    consumerRecords.forEach {
        log.info("Received consumer record with key: ${it.key()}")
        behandlerDialogmeldingService.handleIncomingDialogmeldingBestilling(it.value())
    }
}
