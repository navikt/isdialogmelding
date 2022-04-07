package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjon
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
        behandlerArbeidstakerRelasjon: BehandlerArbeidstakerRelasjon,
    ): Behandler {
        val pBehandler = getBehandler(behandler = behandler)
        if (pBehandler == null) {
            return createBehandlerForArbeidstaker(
                behandler = behandler,
                behandlerArbeidstakerRelasjon = behandlerArbeidstakerRelasjon,
            )
        }

        val pBehandlereForArbeidstakerIdList =
            database.getBehandlerForArbeidstaker(
                personIdentNumber = behandlerArbeidstakerRelasjon.arbeidstakerPersonident,
            ).map { it.id }

        val isBytteAvBehandler = pBehandlereForArbeidstakerIdList.firstOrNull() != pBehandler.id
        val behandlerIkkeKnyttetTilArbeidstaker = !pBehandlereForArbeidstakerIdList.contains(pBehandler.id)
        if (isBytteAvBehandler || behandlerIkkeKnyttetTilArbeidstaker) {
            addBehandlerToArbeidstaker(
                behandlerArbeidstakerRelasjon = behandlerArbeidstakerRelasjon,
                behandlerId = pBehandler.id,
            )
        }

        return pBehandler.toBehandler(
            kontor = database.getBehandlerKontorForId(pBehandler.kontorId)
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
        behandlerArbeidstakerRelasjon: BehandlerArbeidstakerRelasjon,
        behandler: Behandler,
    ): Behandler {
        database.connection.use { connection ->
            val pBehandlerKontor = connection.getBehandlerKontorForPartnerId(behandler.kontor.partnerId)
            val kontorId = if (pBehandlerKontor != null) {
                pBehandlerKontor.id
            } else {
                connection.createBehandlerKontor(behandler.kontor)
            }
            val pBehandler = connection.createBehandler(
                behandler = behandler,
                kontorId = kontorId,
            )
            connection.createBehandlerArbeidstakerRelasjon(
                behandlerArbeidstakerRelasjon = behandlerArbeidstakerRelasjon,
                behandlerId = pBehandler.id,
            )
            connection.commit()

            return pBehandler.toBehandler(
                database.getBehandlerKontorForId(pBehandler.kontorId)
            )
        }
    }

    private fun addBehandlerToArbeidstaker(
        behandlerArbeidstakerRelasjon: BehandlerArbeidstakerRelasjon,
        behandlerId: Int,
    ) {
        database.connection.use { connection ->
            connection.createBehandlerArbeidstakerRelasjon(
                behandlerArbeidstakerRelasjon = behandlerArbeidstakerRelasjon,
                behandlerId = behandlerId,
            )
            connection.commit()
        }
    }
}
