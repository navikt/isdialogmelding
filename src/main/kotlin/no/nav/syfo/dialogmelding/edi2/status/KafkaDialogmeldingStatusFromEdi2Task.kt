package no.nav.syfo.dialogmelding.edi2.status

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.kafka.ApplicationEnvironmentKafka
import no.nav.syfo.application.launchBackgroundTask
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

const val HELSEMELDING_DIALOG_OUT_STATUS_TOPIC = "helsemelding.dialog.out.status"

fun launchKafkaTaskDialogmeldingStatusFromEdi2(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
    edi2DialogmeldingStatusConsumerService: Edi2DialogmeldingStatusConsumerService,
) {
    launchBackgroundTask(
        applicationState = applicationState
    ) {
        val consumerProperties = kafkaDialogmeldingStatusFromEdi2ConsumerConfig(applicationEnvironmentKafka)
        val kafkaConsumer = KafkaConsumer<String, KafkaDialogmeldingStatusFromEdi2DTO>(consumerProperties)
        kafkaConsumer.subscribe(listOf(HELSEMELDING_DIALOG_OUT_STATUS_TOPIC))
        while (applicationState.ready) {
            val records = kafkaConsumer.poll(Duration.ofMillis(1000))
            if (records.count() > 0) {
                records.forEach { record ->
                    record.value()?.let { statusMessage ->
                        edi2DialogmeldingStatusConsumerService.handleIncomingStatus(statusMessage)
                    } ?: log.warn("Got tombstone from {}", HELSEMELDING_DIALOG_OUT_STATUS_TOPIC)
                }
                kafkaConsumer.commitSync()
            }
        }
    }
}

private val log = LoggerFactory.getLogger("no.nav.syfo.dialogmelding.edi2.status")
