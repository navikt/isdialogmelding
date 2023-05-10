package no.nav.syfo.dialogmelding.status

import no.nav.syfo.dialogmelding.status.database.createDialogmeldingStatus
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus
import java.sql.Connection

class DialogmeldingStatusService {
    fun createDialogmeldingStatus(connection: Connection, dialogmeldingStatus: DialogmeldingStatus, bestillingId: Int) {
        connection.createDialogmeldingStatus(
            dialogmeldingStatus = dialogmeldingStatus,
            bestillingId = bestillingId
        )
    }
}
