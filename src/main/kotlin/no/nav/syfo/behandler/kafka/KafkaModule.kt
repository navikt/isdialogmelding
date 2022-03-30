package no.nav.syfo.behandler.kafka

import no.nav.syfo.application.*
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.behandler.kafka.behandlerdialogmelding.blockingApplicationLogicDialogmeldingBestilling
import no.nav.syfo.behandler.kafka.sykmelding.blockingApplicationLogicSykmelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka")

fun launchKafkaTaskDialogmeldingBestilling(
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

fun launchKafkaTaskSykmelding(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
) {
    launchBackgroundTask(applicationState = applicationState) {
        blockingApplicationLogicSykmelding(
            applicationState = applicationState,
            applicationEnvironmentKafka = applicationEnvironmentKafka,
        )
    }
}
