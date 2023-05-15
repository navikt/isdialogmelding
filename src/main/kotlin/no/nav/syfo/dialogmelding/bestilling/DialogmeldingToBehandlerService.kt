package no.nav.syfo.dialogmelding.bestilling

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.Arbeidstaker
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.bestilling.database.*
import no.nav.syfo.dialogmelding.bestilling.kafka.*
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus
import no.nav.syfo.domain.Personident
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.dialogmelding.bestilling")

class DialogmeldingToBehandlerService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
    private val dialogmeldingStatusService: DialogmeldingStatusService,
) {
    fun getBestillinger(): List<Pair<Int, DialogmeldingToBehandlerBestilling>> {
        return database.getDialogmeldingToBehandlerBestillingNotSendt().map(::toDialogmeldingToBehandlerBestillingPair)
    }

    fun getBestilling(uuid: UUID): Pair<Int, DialogmeldingToBehandlerBestilling>? {
        return database.getBestilling(uuid = uuid)?.let(::toDialogmeldingToBehandlerBestillingPair)
    }

    private fun toDialogmeldingToBehandlerBestillingPair(
        pDialogmeldingToBehandlerBestilling: PDialogmeldingToBehandlerBestilling,
    ): Pair<Int, DialogmeldingToBehandlerBestilling> = Pair(
        pDialogmeldingToBehandlerBestilling.id,
        withBehandler(pDialogmeldingToBehandlerBestilling)
    )

    private fun withBehandler(
        pDialogmeldingToBehandlerBestilling: PDialogmeldingToBehandlerBestilling,
    ): DialogmeldingToBehandlerBestilling {
        val pBehandler = database.getBehandlerById(pDialogmeldingToBehandlerBestilling.behandlerId)!!
        return pDialogmeldingToBehandlerBestilling.toDialogmeldingToBehandlerBestilling(
            pBehandler.toBehandler(
                kontor = database.getBehandlerKontorById(pBehandler.kontorId)
            )
        )
    }

    fun setDialogmeldingBestillingSendt(uuid: UUID) {
        database.setBehandlerDialogmeldingBestillingSendt(uuid)
    }

    fun incrementDialogmeldingBestillingSendtTries(uuid: UUID) {
        database.incrementDialogmeldingBestillingSendtTries(uuid)
    }

    suspend fun getArbeidstakerIfRelasjonToBehandler(
        behandlerRef: UUID,
        personident: Personident,
    ): Arbeidstaker {
        val pBehandlerArbeidstaker = database.getBehandlerArbeidstakerRelasjon(
            personident = personident,
            behandlerRef = behandlerRef,
        ).first()
        val pdlNavn = pdlClient.person(personident)?.hentPerson?.navn?.first()
            ?: throw RuntimeException("PDL returned empty response")

        return pBehandlerArbeidstaker.toArbeidstaker(
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
            val dialogmeldingToBehandlerBestilling =
                dialogmeldingToBehandlerBestillingDTO.toDialogmeldingToBehandlerBestilling(
                    behandler = pBehandler.toBehandler(
                        kontor = database.getBehandlerKontorById(pBehandler.kontorId)
                    )
                )
            database.connection.use { connection ->
                val pDialogmeldingToBehandlerBestilling = connection.getBestilling(
                    uuid = dialogmeldingToBehandlerBestilling.uuid
                )

                if (pDialogmeldingToBehandlerBestilling == null) {
                    val bestillingId = connection.createBehandlerDialogmeldingBestilling(
                        dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestilling,
                        behandlerId = pBehandler.id,
                    )
                    dialogmeldingStatusService.createDialogmeldingStatus(
                        dialogmeldingStatus = DialogmeldingStatus.bestilt(
                            bestilling = dialogmeldingToBehandlerBestilling
                        ),
                        bestillingId = bestillingId,
                        connection = connection,
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
