package no.nav.syfo.cronjob

import no.nav.syfo.application.*
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.dialogmelding.DialogmeldingService

fun cronjobModule(
    applicationState: ApplicationState,
    environment: Environment,
    mqSender: MQSender,
    behandlerDialogmeldingService: BehandlerDialogmeldingService,
    dialogmeldingService: DialogmeldingService,
) {
    val leaderPodClient = LeaderPodClient(
        environment = environment,
    )
    val cronjobRunner = DialogmeldingCronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient,
    )
    val dialogmeldingSendCronjob = DialogmeldingSendCronjob(
        behandlerDialogmeldingService = behandlerDialogmeldingService,
        dialogmeldingService = dialogmeldingService,
        mqSender = mqSender,
    )

    launchBackgroundTask(
        applicationState = applicationState,
    ) {
        cronjobRunner.start(
            cronjob = dialogmeldingSendCronjob
        )
    }
}
