package no.nav.syfo.behandler.kafka

import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

const val DIALOGMELDING_BESTILLING_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka")

fun blockingApplicationLogicDialogmeldingBestilling(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
    database: DatabaseInterface,
) {
    val consumerProperties = kafkaBehandlerDialogmeldingBestillingConsumerConfig(applicationEnvironmentKafka)
    val kafkaConsumerDialogmeldingBestilling = KafkaConsumer<String, KafkaBehandlerDialogmeldingDTO>(consumerProperties)

    kafkaConsumerDialogmeldingBestilling.subscribe(
        listOf(DIALOGMELDING_BESTILLING_TOPIC)
    )
    while (applicationState.ready) {
        pollAndProcessDialogmeldingBestilling(
            database = database,
            kafkaConsumerDialogmeldingBestilling = kafkaConsumerDialogmeldingBestilling,
        )
    }
}

fun pollAndProcessDialogmeldingBestilling(
    database: DatabaseInterface,
    kafkaConsumerDialogmeldingBestilling: KafkaConsumer<String, KafkaBehandlerDialogmeldingDTO>,
) {
    val records = kafkaConsumerDialogmeldingBestilling.poll(Duration.ofMillis(1000))
    if (records.count() > 0) {
        createAndStoreDialogmeldingBestillingFromRecords(
            consumerRecords = records,
            database = database,
        )
        kafkaConsumerDialogmeldingBestilling.commitSync()
    }
}

fun createAndStoreDialogmeldingBestillingFromRecords(
    consumerRecords: ConsumerRecords<String, KafkaBehandlerDialogmeldingDTO>,
    database: DatabaseInterface,
) {
    // TODO: Store and send dialogmeldinger
    consumerRecords.forEach {
        log.info("Received consumer record with key: ${it.key()} behandlerRef: ${it.value().behandlerRef} kode: ${it.value().dialogmeldingKode} parent: ${it.value().dialogmeldingRefParent} conversation: ${it.value().dialogmeldingRefConversation}")
    }
}
