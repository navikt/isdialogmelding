package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.createBehandlerDialogmelding
import no.nav.syfo.behandler.database.createBehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.database.domain.toBehandler
import no.nav.syfo.behandler.database.getBehandlerDialogmelding
import no.nav.syfo.behandler.database.getBehandlerDialogmeldingForArbeidstaker
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.Fastlege
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.toFastlege
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.domain.PersonIdentNumber

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
        val aktivFastlege = getFastlegeMedPartnerinfo(
            personIdentNumber = personIdentNumber,
            token = token,
            callId = callId,
        )

        return if (aktivFastlege != null) {
            val behandler = createOrGetBehandler(
                fastlege = aktivFastlege,
                personIdentNumber = personIdentNumber,
            )

            listOf(behandler)
        } else emptyList()
    }

    private suspend fun getFastlegeMedPartnerinfo(
        personIdentNumber: PersonIdentNumber,
        token: String,
        callId: String,
    ): Fastlege? {
        val fastlegeResponse = fastlegeClient.fastlege(
            personIdentNumber = personIdentNumber,
            token = token,
            callId = callId,
        )

        if (fastlegeResponse?.foreldreEnhetHerId == null) {
            return null
        }

        val partnerinfoResponse = partnerinfoClient.partnerinfo(
            herId = fastlegeResponse.foreldreEnhetHerId.toString(),
            token = token,
            callId = callId,
        )

        return if (partnerinfoResponse != null) {
            fastlegeResponse.toFastlege(partnerinfoResponse.partnerId)
        } else null
    }

    private fun createOrGetBehandler(fastlege: Fastlege, personIdentNumber: PersonIdentNumber): Behandler {
        val pBehandlerForFastlege = database.getBehandlerDialogmelding(partnerId = fastlege.partnerId)
        if (pBehandlerForFastlege == null) {
            return createBehandlerDialogmelding(
                personIdentNumber = personIdentNumber,
                fastlege = fastlege,
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

    fun createBehandlerDialogmelding(
        personIdentNumber: PersonIdentNumber,
        fastlege: Fastlege,
    ): Behandler {
        database.connection.use { connection ->
            val pBehandlerForFastlege = connection.createBehandlerDialogmelding(
                fastlege = fastlege,
            )
            connection.createBehandlerDialogmeldingArbeidstaker(
                personIdentNumber = personIdentNumber,
                behandlerDialogmeldingId = pBehandlerForFastlege.id,
            )
            connection.commit()

            return pBehandlerForFastlege.toBehandler()
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
