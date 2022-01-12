package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.PBehandlerDialogmelding
import no.nav.syfo.behandler.database.domain.toBehandler
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.hasRequiredIds
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
    suspend fun getBehandlere(
        personIdentNumber: PersonIdentNumber,
        token: String,
        callId: String,
    ): List<Behandler> {
        val aktivFastlegeBehandler = getAktivFastlegeBehandler(
            personIdentNumber = personIdentNumber,
            token = token,
            callId = callId,
        )

        if (aktivFastlegeBehandler != null && aktivFastlegeBehandler.hasRequiredIds()) {
            val behandler = createOrGetBehandler(
                behandler = aktivFastlegeBehandler,
                personIdentNumber = personIdentNumber,
            )

            return listOf(behandler)
        }

        return emptyList()
    }

    private suspend fun getAktivFastlegeBehandler(
        personIdentNumber: PersonIdentNumber,
        token: String,
        callId: String,
    ): Behandler? {
        val fastlegeResponse = fastlegeClient.fastlege(
            personIdentNumber = personIdentNumber,
            token = token,
            callId = callId,
        ) ?: return null

        if (fastlegeResponse.foreldreEnhetHerId == null) {
            log.warn("Aktiv fastlege missing foreldreEnhetHerId so cannot request partnerinfo")
            return null
        }

        val partnerinfoResponse = partnerinfoClient.partnerinfo(
            herId = fastlegeResponse.foreldreEnhetHerId.toString(),
            token = token,
            callId = callId,
        )

        return if (partnerinfoResponse != null) {
            fastlegeResponse.toBehandler(
                partnerId = partnerinfoResponse.partnerId,
            )
        } else null
    }

    private fun createOrGetBehandler(
        behandler: Behandler,
        personIdentNumber: PersonIdentNumber,
    ): Behandler {
        val pBehandlerForFastlege = getBehandlerDialogmelding(behandler = behandler)
        if (pBehandlerForFastlege == null) {
            return createBehandlerDialogmelding(
                behandler = behandler,
                personIdentNumber = personIdentNumber
            )
        }

        val pBehandlereForArbeidstakerIdList =
            database.getBehandlerDialogmeldingForArbeidstaker(personIdentNumber = personIdentNumber).map { it.id }

        val isBytteAvFastlege = pBehandlereForArbeidstakerIdList.firstOrNull() != pBehandlerForFastlege.id
        val behandlerIkkeKnyttetTilArbeidstaker = !pBehandlereForArbeidstakerIdList.contains(pBehandlerForFastlege.id)
        if (isBytteAvFastlege || behandlerIkkeKnyttetTilArbeidstaker) {
            addBehandlerDialogmeldingToArbeidstaker(
                personIdentNumber = personIdentNumber,
                behandlerDialogmeldingId = pBehandlerForFastlege.id,
            )
        }

        return pBehandlerForFastlege.toBehandler()
    }

    private fun getBehandlerDialogmelding(behandler: Behandler): PBehandlerDialogmelding? {
        if (behandler.personident != null) {
            return database.getBehandlerDialogmeldingMedPersonIdentForPartnerId(
                behandlerPersonIdent = behandler.personident,
                partnerId = behandler.partnerId,
            )
        } else if (behandler.hprId != null) {
            return database.getBehandlerDialogmeldingMedHprIdForPartnerId(
                hprId = behandler.hprId,
                partnerId = behandler.partnerId,
            )
        } else if (behandler.herId != null) {
            return database.getBehandlerDialogmeldingMedHerIdForPartnerId(
                herId = behandler.herId,
                partnerId = behandler.partnerId,
            )
        }

        throw IllegalArgumentException("Behandler missing personident, hprId and herId")
    }

    private fun createBehandlerDialogmelding(
        personIdentNumber: PersonIdentNumber,
        behandler: Behandler,
    ): Behandler {
        database.connection.use { connection ->
            val pBehandlerDialogmelding = connection.createBehandlerDialogmelding(
                behandler = behandler,
            )
            connection.createBehandlerDialogmeldingArbeidstaker(
                personIdentNumber = personIdentNumber,
                behandlerDialogmeldingId = pBehandlerDialogmelding.id,
            )
            connection.commit()

            return pBehandlerDialogmelding.toBehandler()
        }
    }

    private fun addBehandlerDialogmeldingToArbeidstaker(
        personIdentNumber: PersonIdentNumber,
        behandlerDialogmeldingId: Int,
    ) {
        database.connection.use { connection ->
            connection.createBehandlerDialogmeldingArbeidstaker(
                personIdentNumber = personIdentNumber,
                behandlerDialogmeldingId = behandlerDialogmeldingId,
            )
            connection.commit()
        }
    }
}
