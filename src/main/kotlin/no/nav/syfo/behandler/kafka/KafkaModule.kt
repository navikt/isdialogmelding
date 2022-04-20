package no.nav.syfo.behandler.kafka

import no.nav.syfo.application.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler.blockingApplicationLogicDialogmeldingFromBehandler
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.blockingApplicationLogicDialogmeldingBestilling
import no.nav.syfo.behandler.kafka.sykmelding.blockingApplicationLogicSykmelding
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka")

fun launchKafkaTaskDialogmeldingBestilling(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
) {
    launchBackgroundTask(applicationState = applicationState) {
        blockingApplicationLogicDialogmeldingBestilling(
            applicationState = applicationState,
            applicationEnvironmentKafka = applicationEnvironmentKafka,
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
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

fun launchKafkaTaskDialogmeldingFromBehandler(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
    database: DatabaseInterface,
) {
    launchBackgroundTask(applicationState = applicationState) {
        blockingApplicationLogicDialogmeldingFromBehandler(
            applicationState = applicationState,
            applicationEnvironmentKafka = applicationEnvironmentKafka,
            database = database,
        )
    }
}
