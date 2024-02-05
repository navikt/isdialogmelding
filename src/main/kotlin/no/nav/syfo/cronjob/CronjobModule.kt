package no.nav.syfo.cronjob

import no.nav.syfo.application.*
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.btsys.LegeSuspensjonClient
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.dialogmelding.DialogmeldingService
import no.nav.syfo.dialogmelding.cronjob.DialogmeldingStatusCronjob
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService

fun cronjobModule(
    applicationState: ApplicationState,
    environment: Environment,
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    dialogmeldingService: DialogmeldingService,
    dialogmeldingStatusService: DialogmeldingStatusService,
    behandlerService: BehandlerService,
    partnerinfoClient: PartnerinfoClient,
    legeSuspensjonClient: LegeSuspensjonClient,
    fastlegeClient: FastlegeClient,
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
    val dialogmeldingStatusCronjob =
        DialogmeldingStatusCronjob(dialogmeldingStatusService = dialogmeldingStatusService)

    val verifyPartnerIdCronjob = VerifyPartnerIdCronjob(
        behandlerService = behandlerService,
        partnerinfoClient = partnerinfoClient,
    )

    val suspensjonCronjob = SuspensjonCronjob(
        behandlerService = behandlerService,
        legeSuspensjonClient = legeSuspensjonClient,
    )

    val verifyBehandlereForKontorCronjob = VerifyBehandlereForKontorCronjob(
        behandlerService = behandlerService,
        fastlegeClient = fastlegeClient,
    )

    listOf(
        dialogmeldingSendCronjob,
        dialogmeldingStatusCronjob,
        verifyPartnerIdCronjob,
        suspensjonCronjob,
        verifyBehandlereForKontorCronjob,
    ).forEach {
        launchBackgroundTask(
            applicationState = applicationState,
        ) {
            cronjobRunner.start(
                cronjob = it
            )
        }
    }
}
