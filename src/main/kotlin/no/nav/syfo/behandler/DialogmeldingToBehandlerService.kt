package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.*
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdentNumber
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler")

class DialogmeldingToBehandlerService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {
    fun getBestillinger(): List<DialogmeldingToBehandlerBestilling> {
        return database.getDialogmeldingToBehandlerBestillingNotSendt()
            .map { pBehandlerDialogMeldingBestilling ->
                pBehandlerDialogMeldingBestilling.toDialogmeldingToBehandlerBestilling(
                    database.getBehandlerDialogmeldingForId(pBehandlerDialogMeldingBestilling.behandlerId)!!.toBehandler()
                )
            }
    }

    fun setDialogmeldingBestillingSendt(uuid: UUID) {
        database.setBehandlerDialogmeldingBestillingSendt(uuid)
    }

    fun incrementDialogmeldingBestillingSendtTries(uuid: UUID) {
        database.incrementDialogmeldingBestillingSendtTries(uuid)
    }

    suspend fun getBehandlerDialogmeldingArbeidstaker(
        behandlerRef: UUID,
        personIdent: PersonIdentNumber,
    ): BehandlerDialogmeldingArbeidstaker {
        val pBehandlerDialogmeldingArbeidstaker = database.getBehandlerDialogmeldingArbeidstaker(
            personIdentNumber = personIdent,
            behandlerRef = behandlerRef,
        )
        val pdlNavn = pdlClient.person(personIdent)?.hentPerson?.navn?.first()
            ?: throw RuntimeException("PDL returned empty response")
        return pBehandlerDialogmeldingArbeidstaker.toBehandlerDialogmeldingArbeidstaker(
            fornavn = pdlNavn.fornavn,
            mellomnavn = pdlNavn.mellomnavn,
            etternavn = pdlNavn.etternavn,
        )
    }

    fun handleIncomingDialogmeldingBestilling(
        dialogmeldingToBehandlerBestillingDTO: DialogmeldingToBehandlerBestillingDTO,
    ) {
        val behandlerRef = UUID.fromString(dialogmeldingToBehandlerBestillingDTO.behandlerRef)
        val pBehandler = database.getBehandlerDialogmeldingForUuid(behandlerRef)
        if (pBehandler == null) {
            log.error("Unknown behandlerRef $behandlerRef in dialogmeldingToBehandlerBestilling ${dialogmeldingToBehandlerBestillingDTO.dialogmeldingUuid}")
        } else {
            val dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestillingDTO.toDialogmeldingToBehandlerBestilling(
                behandler = pBehandler.toBehandler()
            )
            database.connection.use { connection ->
                val pBehandlerDialogmeldingBestilling = connection.getBestillinger(
                    uuid = dialogmeldingToBehandlerBestilling.uuid
                )

                if (pBehandlerDialogmeldingBestilling == null) {
                    connection.createBehandlerDialogmeldingBestilling(
                        dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestilling,
                        behandlerId = pBehandler.id,
                    )
                } else {
                    log.warn("Ignoring duplicate behandler dialogmelding bestilling with uuid: ${dialogmeldingToBehandlerBestilling.uuid}.")
                }
                connection.commit()
            }
        }
    }
}
