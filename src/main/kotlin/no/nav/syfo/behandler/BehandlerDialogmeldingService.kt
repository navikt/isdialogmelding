package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import no.nav.syfo.behandler.kafka.behandlerdialogmelding.*
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdentNumber
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler")

class BehandlerDialogmeldingService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {
    fun getDialogmeldingBestillingListe(): List<BehandlerDialogmeldingBestilling> {
        return database.getBehandlerDialogmeldingBestillingNotSendt()
            .map { pBehandlerDialogMeldingBestilling ->
                pBehandlerDialogMeldingBestilling.toBehandlerDialogmeldingBestilling(
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

    suspend fun getBehandlerDialogmeldingArbeidstaker(personIdent: PersonIdentNumber): BehandlerDialogmeldingArbeidstaker {
        val pdlNavn = pdlClient.person(personIdent)?.hentPerson?.navn?.first()
            ?: throw RuntimeException("PDL returned empty response")
        return BehandlerDialogmeldingArbeidstaker(
            arbeidstakerPersonident = personIdent,
            fornavn = pdlNavn.fornavn,
            mellomnavn = pdlNavn.mellomnavn,
            etternavn = pdlNavn.etternavn,
        )
    }

    fun handleIncomingDialogmeldingBestilling(
        dialogmeldingBestillingDTO: BehandlerDialogmeldingBestillingDTO,
    ) {
        val behandlerRef = UUID.fromString(dialogmeldingBestillingDTO.behandlerRef)
        val pBehandler = database.getBehandlerDialogmeldingForUuid(behandlerRef)
        if (pBehandler == null) {
            log.error("Unknown behandlerRef $behandlerRef in behandlerDialogmeldingBestilling ${dialogmeldingBestillingDTO.dialogmeldingUuid}")
        } else {
            val behandlerDialogmeldingBestilling = dialogmeldingBestillingDTO.toBehandlerDialogmeldingBestilling(
                behandler = pBehandler.toBehandler()
            )
            database.connection.use { connection ->
                val pBehandlerDialogmeldingBestilling = connection.getBehandlerDialogmeldingBestilling(
                    uuid = behandlerDialogmeldingBestilling.uuid
                )

                if (pBehandlerDialogmeldingBestilling == null) {
                    connection.createBehandlerDialogmeldingBestilling(
                        behandlerDialogmeldingBestilling = behandlerDialogmeldingBestilling,
                        behandlerId = pBehandler.id,
                    )
                } else {
                    log.warn("Ignoring duplicate behandler dialogmelding bestilling with uuid: ${behandlerDialogmeldingBestilling.uuid}.")
                }
                connection.commit()
            }
        }
    }
}
