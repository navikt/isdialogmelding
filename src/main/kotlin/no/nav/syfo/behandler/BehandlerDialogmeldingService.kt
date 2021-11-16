package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.toBehandlerDialogmeldingBestilling
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import java.util.UUID

class BehandlerDialogmeldingService(
    private val database: DatabaseInterface,
) {
    fun getDialogmeldingBestillingListe(): List<BehandlerDialogmeldingBestilling> {
        return database.getBehandlerDialogmeldingBestillingNotSendt()
            .map { pBehandlerDialogMeldingBestilling ->
                pBehandlerDialogMeldingBestilling.toBehandlerDialogmeldingBestilling(
                    database.getBehandlerDialogmeldingForId(pBehandlerDialogMeldingBestilling.behandlerId)!!.behandlerRef
                )
            }
    }

    fun setDialogmeldingBestillingSendt(uuid: UUID) {
        database.setBehandlerDialogmeldingBestillingSendt(uuid)
    }

    fun incrementDialogmeldingBestillingSendtTries(uuid: UUID) {
        database.incrementDialogmeldingBestillingSendtTries(uuid)
    }
}
