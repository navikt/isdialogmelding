package no.nav.syfo.cronjob

import no.nav.syfo.application.*
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.dialogmelding.DialogmeldingService
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService

fun cronjobModule(
    applicationState: ApplicationState,
    environment: Environment,
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    dialogmeldingService: DialogmeldingService,
    dialogmeldingStatusService: DialogmeldingStatusService,
) {
    val leaderPodClient = LeaderPodClient(
        environment = environment,
    )
    val cronjobRunner = DialogmeldingCronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient,
    )
    val dialogmeldingSendCronjob = DialogmeldingSendCronjob(
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        dialogmeldingService = dialogmeldingService,
        dialogmeldingStatusService = dialogmeldingStatusService,
    )

    launchBackgroundTask(
        applicationState = applicationState,
    ) {
        cronjobRunner.start(
            cronjob = dialogmeldingSendCronjob
        )
    }
}
