package no.nav.syfo.dialogmelding.apprec

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.invalidateBehandler
import no.nav.syfo.dialogmelding.apprec.database.createApprec
import no.nav.syfo.dialogmelding.apprec.database.getApprec
import no.nav.syfo.dialogmelding.apprec.domain.*
import no.nav.syfo.dialogmelding.status.database.createDialogmeldingStatus
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus
import java.util.*

class ApprecService(
    private val database: DatabaseInterface,
) {

    internal fun apprecExists(uuid: UUID): Boolean = database.getApprec(uuid) != null

    internal fun createApprec(
        apprec: Apprec,
        bestillingId: Int,
    ) {
        database.connection.use { connection ->
            connection.createApprec(
                apprec = apprec,
                bestillingId = bestillingId,
            )
            if (apprec.isUkjentMottaker()) {
                connection.invalidateBehandler(apprec.bestilling.behandler.behandlerRef)
            }
            connection.createDialogmeldingStatus(
                dialogmeldingStatus = DialogmeldingStatus.fromApprec(apprec),
                bestillingId = bestillingId,
            )
            connection.commit()
        }
    }
}
