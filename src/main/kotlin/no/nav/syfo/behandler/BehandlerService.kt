package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.domain.PersonIdentNumber
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.syfo.behandler")

class BehandlerService(
    private val fastlegeClient: FastlegeClient,
    private val partnerinfoClient: PartnerinfoClient,
    private val database: DatabaseInterface,
) {

    suspend fun getAktivFastlegeMedPartnerId(
        personIdentNumber: PersonIdentNumber,
        token: String,
        callId: String,
        systemRequest: Boolean = false,
    ): Behandler? {
        val fastlegeResponse = fastlegeClient.fastlege(
            personIdentNumber = personIdentNumber,
            systemRequest = systemRequest,
            token = token,
            callId = callId,
        ) ?: return null

        if (fastlegeResponse.foreldreEnhetHerId == null) {
            log.warn("Aktiv fastlege missing foreldreEnhetHerId so cannot request partnerinfo")
            return null
        }

        val partnerinfoResponse = partnerinfoClient.partnerinfo(
            herId = fastlegeResponse.foreldreEnhetHerId.toString(),
            systemRequest = systemRequest,
            token = token,
            callId = callId,
        )

        return if (partnerinfoResponse != null) {
            fastlegeResponse.toBehandler(
                partnerId = partnerinfoResponse.partnerId,
            )
        } else null
    }

    fun createOrGetBehandler(
        behandler: Behandler,
        behandlerDialogmeldingArbeidstaker: BehandlerDialogmeldingArbeidstaker,
    ): Behandler {
        val pBehandler = getBehandler(behandler = behandler)
        if (pBehandler == null) {
            return createBehandlerForArbeidstaker(
                behandler = behandler,
                behandlerDialogmeldingArbeidstaker = behandlerDialogmeldingArbeidstaker,
            )
        }

        val pBehandlereForArbeidstakerIdList =
            database.getBehandlerForArbeidstaker(
                personIdentNumber = behandlerDialogmeldingArbeidstaker.arbeidstakerPersonident,
            ).map { it.id }

        val isBytteAvBehandler = pBehandlereForArbeidstakerIdList.firstOrNull() != pBehandler.id
        val behandlerIkkeKnyttetTilArbeidstaker = !pBehandlereForArbeidstakerIdList.contains(pBehandler.id)
        if (isBytteAvBehandler || behandlerIkkeKnyttetTilArbeidstaker) {
            addBehandlerToArbeidstaker(
                behandlerDialogmeldingArbeidstaker = behandlerDialogmeldingArbeidstaker,
                behandlerId = pBehandler.id,
            )
        }

        return pBehandler.toBehandler(
            kontor = database.getBehandlerDialogmeldingKontorForId(pBehandler.kontorId)
        )
    }

    private fun getBehandler(behandler: Behandler): PBehandler? {
        return when {
            behandler.personident != null -> database.getBehandlerMedPersonIdentForPartnerId(
                behandlerPersonIdent = behandler.personident,
                partnerId = behandler.kontor.partnerId,
            )
            behandler.hprId != null -> database.getBehandlerMedHprIdForPartnerId(
                hprId = behandler.hprId,
                partnerId = behandler.kontor.partnerId,
            )
            behandler.herId != null -> database.getBehandlerMedHerIdForPartnerId(
                herId = behandler.herId,
                partnerId = behandler.kontor.partnerId,
            )
            else -> throw IllegalArgumentException("Behandler missing personident, hprId and herId")
        }
    }

    private fun createBehandlerForArbeidstaker(
        behandlerDialogmeldingArbeidstaker: BehandlerDialogmeldingArbeidstaker,
        behandler: Behandler,
    ): Behandler {
        database.connection.use { connection ->
            val pBehandlerKontor = connection.getBehandlerDialogmeldingKontorForPartnerId(behandler.kontor.partnerId)
            val kontorId = if (pBehandlerKontor != null) {
                pBehandlerKontor.id
            } else {
                connection.createBehandlerDialogmeldingKontor(behandler.kontor)
            }
            val pBehandlerDialogmelding = connection.createBehandler(
                behandler = behandler,
                kontorId = kontorId,
            )
            connection.createBehandlerDialogmeldingArbeidstaker(
                behandlerDialogmeldingArbeidstaker = behandlerDialogmeldingArbeidstaker,
                behandlerDialogmeldingId = pBehandlerDialogmelding.id,
            )
            connection.commit()

            return pBehandlerDialogmelding.toBehandler(
                database.getBehandlerDialogmeldingKontorForId(pBehandlerDialogmelding.kontorId)
            )
        }
    }

    private fun addBehandlerToArbeidstaker(
        behandlerDialogmeldingArbeidstaker: BehandlerDialogmeldingArbeidstaker,
        behandlerId: Int,
    ) {
        database.connection.use { connection ->
            connection.createBehandlerDialogmeldingArbeidstaker(
                behandlerDialogmeldingArbeidstaker = behandlerDialogmeldingArbeidstaker,
                behandlerDialogmeldingId = behandlerId,
            )
            connection.commit()
        }
    }
}
