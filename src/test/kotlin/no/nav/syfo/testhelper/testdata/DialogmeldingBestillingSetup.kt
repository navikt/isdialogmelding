package no.nav.syfo.testhelper.testdata

import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.dialogmelding.bestilling.database.createBehandlerDialogmeldingBestilling
import no.nav.syfo.dialogmelding.bestilling.kafka.DialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.dialogmelding.bestilling.kafka.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.testhelper.TestDatabase
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import java.util.*

fun lagreDialogmeldingBestillingOgBehandler(
    database: TestDatabase,
    dialogmeldingBestillingUuid: UUID,
): Pair<Int, Behandler> {
    val behandler = lagreBehandler(database)
    val dialogmeldingToBehandlerBestillingDTO = generateDialogmeldingToBehandlerBestillingDTO(
        uuid = dialogmeldingBestillingUuid,
        behandlerRef = behandler.behandlerRef,
    )
    return Pair(lagreDialogmeldingBestilling(database, behandler, dialogmeldingToBehandlerBestillingDTO), behandler)
}

fun lagreBehandler(database: TestDatabase): Behandler {
    val random = Random()
    val behandlerRef = UUID.randomUUID()
    val partnerId = PartnerId(random.nextInt())
    return generateBehandler(behandlerRef, partnerId).also { behandler ->
        database.connection.use { connection ->
            val kontorId = connection.createBehandlerKontor(behandler.kontor)
            connection.createBehandler(behandler, kontorId).id.also {
                connection.commit()
            }
        }
    }
}

fun lagreDialogmeldingBestilling(
    database: TestDatabase,
    behandler: Behandler,
    dialogmeldingToBehandlerBestillingDTO: DialogmeldingToBehandlerBestillingDTO,
): Int {
    val dialogmeldingToBehandlerBestilling =
        dialogmeldingToBehandlerBestillingDTO.toDialogmeldingToBehandlerBestilling(behandler)
    val behandlerId = database.getBehandlerByBehandlerRef(behandler.behandlerRef)!!.id
    val bestillingId = database.connection.use { connection ->
        connection.createBehandlerDialogmeldingBestilling(
            dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestilling,
            behandlerId = behandlerId,
        ).also {
            connection.commit()
        }
    }
    return bestillingId
}
