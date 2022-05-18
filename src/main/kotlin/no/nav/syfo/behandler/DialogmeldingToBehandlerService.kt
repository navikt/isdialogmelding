package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjon
import no.nav.syfo.behandler.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.*
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.Personident
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
            .map { pDialogmeldingToBehandlerBestilling ->
                val pBehandler = database.getBehandlerById(pDialogmeldingToBehandlerBestilling.behandlerId)!!
                pDialogmeldingToBehandlerBestilling.toDialogmeldingToBehandlerBestilling(
                    pBehandler.toBehandler(
                        kontor = database.getBehandlerKontorById(pBehandler.kontorId)
                    )
                )
            }
    }

    fun setDialogmeldingBestillingSendt(uuid: UUID) {
        database.setBehandlerDialogmeldingBestillingSendt(uuid)
    }

    fun incrementDialogmeldingBestillingSendtTries(uuid: UUID) {
        database.incrementDialogmeldingBestillingSendtTries(uuid)
    }

    suspend fun getBehandlerArbeidstakerRelasjon(
        behandlerRef: UUID,
        personident: Personident,
    ): BehandlerArbeidstakerRelasjon {
        val pBehandlerArbeidstaker = database.getBehandlerArbeidstakerRelasjon(
            personident = personident,
            behandlerRef = behandlerRef,
        ).first()
        val pdlNavn = pdlClient.person(personident)?.hentPerson?.navn?.first()
            ?: throw RuntimeException("PDL returned empty response")
        return pBehandlerArbeidstaker.toBehandlerArbeidstakerRelasjon(
            fornavn = pdlNavn.fornavn,
            mellomnavn = pdlNavn.mellomnavn,
            etternavn = pdlNavn.etternavn,
        )
    }

    fun handleIncomingDialogmeldingBestilling(
        dialogmeldingToBehandlerBestillingDTO: DialogmeldingToBehandlerBestillingDTO,
    ) {
        val behandlerRef = UUID.fromString(dialogmeldingToBehandlerBestillingDTO.behandlerRef)
        val pBehandler = database.getBehandlerByBehandlerRef(behandlerRef)
        if (pBehandler == null) {
            log.error("Unknown behandlerRef $behandlerRef in dialogmeldingToBehandlerBestilling ${dialogmeldingToBehandlerBestillingDTO.dialogmeldingUuid}")
        } else {
            val dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestillingDTO.toDialogmeldingToBehandlerBestilling(
                behandler = pBehandler.toBehandler(
                    kontor = database.getBehandlerKontorById(pBehandler.kontorId)
                )
            )
            database.connection.use { connection ->
                val pDialogmeldingToBehandlerBestilling = connection.getBestillinger(
                    uuid = dialogmeldingToBehandlerBestilling.uuid
                )

                if (pDialogmeldingToBehandlerBestilling == null) {
                    connection.createBehandlerDialogmeldingBestilling(
                        dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestilling,
                        behandlerId = pBehandler.id,
                    )
                } else {
                    log.warn("Ignoring duplicate behandler dialogmelding bestilling with uuid: ${dialogmeldingToBehandlerBestilling.uuid}.")
                    COUNT_KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_DUPLICATE.increment()
                }
                connection.commit()
            }
        }
    }
}
