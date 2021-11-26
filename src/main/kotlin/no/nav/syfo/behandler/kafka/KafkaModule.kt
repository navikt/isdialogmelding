package no.nav.syfo.behandler.kafka

import no.nav.syfo.application.*
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka")

fun launchKafkaTask(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
    behandlerDialogmeldingService: BehandlerDialogmeldingService,
) {
    launchBackgroundTask(applicationState = applicationState) {
        blockingApplicationLogicDialogmeldingBestilling(
            applicationState = applicationState,
            applicationEnvironmentKafka = applicationEnvironmentKafka,
            behandlerDialogmeldingService = behandlerDialogmeldingService,
        )
    }
}
