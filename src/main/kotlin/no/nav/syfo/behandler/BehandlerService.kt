package no.nav.syfo.behandler

import no.nav.syfo.behandler.domain.Fastlege
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.toFastlege
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.domain.PersonIdentNumber

class BehandlerService(
    val fastlegeClient: FastlegeClient,
    val partnerinfoClient: PartnerinfoClient
) {
    suspend fun getFastlegeMedPartnerinfo(
        personIdentNumber: PersonIdentNumber,
        token: String,
        callId: String,
    ): Fastlege? {
        val fastlegeResponse = fastlegeClient.fastlege(
            personIdentNumber = personIdentNumber,
            token = token,
            callId = callId,
        )
        if (fastlegeResponse?.foreldreEnhetHerId != null) {
            val partnerinfoResponse = partnerinfoClient.partnerinfo(
                herId = fastlegeResponse.foreldreEnhetHerId.toString(),
                token = token,
                callId = callId,
            )
            if (partnerinfoResponse != null) {
                return fastlegeResponse.toFastlege(partnerinfoResponse.partnerId)
            }
        }

        return null
    }
}
