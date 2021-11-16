package no.nav.syfo.cronjob

import kotlinx.coroutines.*
import no.nav.syfo.application.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(DialogmeldingSendCronjob::class.java)

fun cronjobModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    mqSender: MQSender,
) {
    val leaderPodClient = LeaderPodClient(
        environment = environment,
    )
    val cronjobRunner = DialogmeldingCronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient,
    )
    val behandlerDialogmeldingService = BehandlerDialogmeldingService(
        database = database,
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
