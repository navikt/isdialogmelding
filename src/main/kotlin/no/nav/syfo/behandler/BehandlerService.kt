package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.toBehandler
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.fastlege.*
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
        val aktivFastlegeBehandlerPair = getAktivFastlegeBehandler(
            personIdentNumber = personIdentNumber,
            token = token,
            callId = callId,
        )
        return if (aktivFastlegeBehandlerPair != null) {
            val behandler = createOrGetBehandler(
                behandler = aktivFastlegeBehandlerPair.first,
                pasient = aktivFastlegeBehandlerPair.second,
            )
            listOf(behandler)
        } else emptyList()
    }

    private suspend fun getAktivFastlegeBehandler(
        personIdentNumber: PersonIdentNumber,
        token: String,
        callId: String,
    ): Pair<Behandler, Pasient>? {
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
            fastlegeResponse.toBehandlerPasientPair(
                partnerId = partnerinfoResponse.partnerId,
            )
        } else null
    }

    private fun createOrGetBehandler(
        behandler: Behandler,
        pasient: Pasient,
    ): Behandler {
        val personIdentNumber = PersonIdentNumber(pasient.fnr)
        val pBehandlerForFastlege = database.getBehandlerDialogmeldingForPartnerId(
            partnerId = behandler.partnerId,
        )
        if (pBehandlerForFastlege == null) {
            return createBehandlerDialogmelding(
                behandler = behandler,
                personIdentNumber = personIdentNumber,
                fornavn = pasient.fornavn,
                mellomnavn = pasient.mellomnavn,
                etternavn = pasient.etternavn,
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
                fornavn = pasient.fornavn,
                mellomnavn = pasient.mellomnavn,
                etternavn = pasient.etternavn,
            )
        } else {
            updateBehandlerDialogmeldingToArbeidstaker(
                personIdentNumber = personIdentNumber,
                behandlerDialogmeldingId = pBehandlerForFastlege.id,
                fornavn = pasient.fornavn,
                mellomnavn = pasient.mellomnavn,
                etternavn = pasient.etternavn,
            )
        }

        return pBehandlerForFastlege.toBehandler()
    }

    private fun createBehandlerDialogmelding(
        personIdentNumber: PersonIdentNumber,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        behandler: Behandler,
    ): Behandler {
        database.connection.use { connection ->
            val pBehandlerDialogmelding = connection.createBehandlerDialogmelding(
                behandler = behandler,
            )
            connection.createBehandlerDialogmeldingArbeidstaker(
                personIdentNumber = personIdentNumber,
                fornavn = fornavn,
                mellomnavn = mellomnavn,
                etternavn = etternavn,
                behandlerDialogmeldingId = pBehandlerDialogmelding.id,
            )
            connection.commit()

            return pBehandlerDialogmelding.toBehandler()
        }
    }

    private fun addBehandlerDialogmeldingToArbeidstaker(
        personIdentNumber: PersonIdentNumber,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        behandlerDialogmeldingId: Int,
    ) {
        database.connection.use { connection ->
            connection.createBehandlerDialogmeldingArbeidstaker(
                personIdentNumber = personIdentNumber,
                fornavn = fornavn,
                mellomnavn = mellomnavn,
                etternavn = etternavn,
                behandlerDialogmeldingId = behandlerDialogmeldingId,
            )
            connection.commit()
        }
    }

    private fun updateBehandlerDialogmeldingToArbeidstaker(
        personIdentNumber: PersonIdentNumber,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        behandlerDialogmeldingId: Int,
    ) {
        database.connection.use { connection ->
            connection.updateBehandlerDialogmeldingArbeidstaker(
                personIdentNumber = personIdentNumber,
                fornavn = fornavn,
                mellomnavn = mellomnavn,
                etternavn = etternavn,
                behandlerDialogmeldingId = behandlerDialogmeldingId,
            )
            connection.commit()
        }
    }
}
