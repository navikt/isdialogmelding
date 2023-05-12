package no.nav.syfo.testhelper.testdata

import no.nav.syfo.dialogmelding.bestilling.kafka.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.dialogmelding.status.database.createDialogmeldingStatus
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus
import no.nav.syfo.testhelper.TestDatabase
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import java.time.OffsetDateTime
import java.util.*

fun lagreDialogmeldingStatusBestiltOgSendt(database: TestDatabase) {
    val behandler = lagreBehandler(database)
    val dialogmeldingBestillingDTO = generateDialogmeldingToBehandlerBestillingDTO(
        uuid = UUID.randomUUID(),
        behandlerRef = behandler.behandlerRef,
    )
    val bestillingId =
        lagreDialogmeldingBestilling(database, behandler, dialogmeldingBestillingDTO)
    val dialogmeldingToBehandlerBestilling =
        dialogmeldingBestillingDTO.toDialogmeldingToBehandlerBestilling(behandler)

    val dialogmeldingStatusBestilt = DialogmeldingStatus.bestilt(
        dialogmeldingToBehandlerBestilling
    ).copy(
        createdAt = OffsetDateTime.now().minusMinutes(30)
    )
    val dialogmeldingStatusSendt = DialogmeldingStatus.sendt(
        dialogmeldingToBehandlerBestilling
    )
    database.createDialogmeldingStatus(
        dialogmeldingStatus = dialogmeldingStatusBestilt,
        bestillingId = bestillingId
    )
    database.createDialogmeldingStatus(
        dialogmeldingStatus = dialogmeldingStatusSendt,
        bestillingId = bestillingId
    )
}
