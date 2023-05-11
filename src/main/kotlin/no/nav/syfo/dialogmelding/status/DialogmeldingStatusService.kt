package no.nav.syfo.dialogmelding.status

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.dialogmelding.status.database.createDialogmeldingStatus
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus
import java.sql.Connection

class DialogmeldingStatusService(
    private val database: DatabaseInterface,
) {
    fun createDialogmeldingStatus(
        dialogmeldingStatus: DialogmeldingStatus,
        bestillingId: Int,
        connection: Connection? = null,
    ) {
        database.createDialogmeldingStatus(
            dialogmeldingStatus = dialogmeldingStatus,
            bestillingId = bestillingId,
            connection = connection,
        )
    }
}
