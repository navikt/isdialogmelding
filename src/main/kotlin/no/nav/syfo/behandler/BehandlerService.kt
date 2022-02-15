package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.PBehandlerDialogmelding
import no.nav.syfo.behandler.database.domain.toBehandler
import no.nav.syfo.behandler.domain.Behandler
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

    fun createOrGetBehandler(behandler: Behandler, arbeidstakerPersonIdent: PersonIdentNumber): Behandler {
        val pBehandler = getBehandler(behandler = behandler)
        if (pBehandler == null) {
            return createBehandlerForArbeidstaker(
                behandler = behandler,
                arbeidstakerPersonIdent = arbeidstakerPersonIdent
            )
        }

        val pBehandlereForArbeidstakerIdList =
            database.getBehandlerDialogmeldingForArbeidstaker(personIdentNumber = arbeidstakerPersonIdent).map { it.id }

        val isBytteAvBehandler = pBehandlereForArbeidstakerIdList.firstOrNull() != pBehandler.id
        val behandlerIkkeKnyttetTilArbeidstaker = !pBehandlereForArbeidstakerIdList.contains(pBehandler.id)
        if (isBytteAvBehandler || behandlerIkkeKnyttetTilArbeidstaker) {
            addBehandlerToArbeidstaker(
                arbeidstakerPersonIdent = arbeidstakerPersonIdent,
                behandlerId = pBehandler.id,
            )
        }

        return pBehandler.toBehandler()
    }

    private fun getBehandler(behandler: Behandler): PBehandlerDialogmelding? {
        return when {
            behandler.personident != null -> database.getBehandlerDialogmeldingMedPersonIdentForPartnerId(
                behandlerPersonIdent = behandler.personident,
                partnerId = behandler.partnerId,
            )
            behandler.hprId != null -> database.getBehandlerDialogmeldingMedHprIdForPartnerId(
                hprId = behandler.hprId,
                partnerId = behandler.partnerId,
            )
            behandler.herId != null -> database.getBehandlerDialogmeldingMedHerIdForPartnerId(
                herId = behandler.herId,
                partnerId = behandler.partnerId,
            )
            else -> throw IllegalArgumentException("Behandler missing personident, hprId and herId")
        }
    }

    private fun createBehandlerForArbeidstaker(
        arbeidstakerPersonIdent: PersonIdentNumber,
        behandler: Behandler,
    ): Behandler {
        database.connection.use { connection ->
            val pBehandlerDialogmelding = connection.createBehandlerDialogmelding(
                behandler = behandler,
            )
            connection.createBehandlerDialogmeldingArbeidstaker(
                personIdentNumber = arbeidstakerPersonIdent,
                behandlerDialogmeldingId = pBehandlerDialogmelding.id,
            )
            connection.commit()

            return pBehandlerDialogmelding.toBehandler()
        }
    }

    private fun addBehandlerToArbeidstaker(
        arbeidstakerPersonIdent: PersonIdentNumber,
        behandlerId: Int,
    ) {
        database.connection.use { connection ->
            connection.createBehandlerDialogmeldingArbeidstaker(
                personIdentNumber = arbeidstakerPersonIdent,
                behandlerDialogmeldingId = behandlerId,
            )
            connection.commit()
        }
    }
}
