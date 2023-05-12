package no.nav.syfo.dialogmelding.status

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.status.database.*
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus

class DialogmeldingStatusService(
    private val database: DatabaseInterface,
    private val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
) {
    fun createDialogmeldingStatus(
        dialogmeldingStatus: DialogmeldingStatus,
        bestillingId: Int,
    ) {
        database.createDialogmeldingStatus(
            dialogmeldingStatus = dialogmeldingStatus,
            bestillingId = bestillingId,
        )
    }

    fun getUnpublishedDialogmeldingStatus(): List<DialogmeldingStatus> {
        return database.getDialogmeldingStatusNotPublished().map { pDialogmeldingStatus ->
            val bestilling = dialogmeldingToBehandlerService.getBestilling(pDialogmeldingStatus.bestillingId)!!
            pDialogmeldingStatus.toDialogmoteStatus(bestilling)
        }
    }

    fun publishDialogmeldingStatus(dialogmeldingStatus: DialogmeldingStatus) {
        // TODO: Publish to kafka topic
        database.updatePublishedAt(uuid = dialogmeldingStatus.uuid)
    }
}
