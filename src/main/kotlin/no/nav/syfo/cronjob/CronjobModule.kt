package no.nav.syfo.cronjob

import no.nav.syfo.application.*
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient

fun cronjobModule(
    applicationState: ApplicationState,
    environment: Environment,
    mqSender: MQSender,
    behandlerDialogmeldingService: BehandlerDialogmeldingService,
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
