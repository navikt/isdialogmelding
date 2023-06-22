package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.behandler.fastlege.FastlegeClient.Companion.FASTLEGEVIKAR_SYSTEM_PATH
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

fun MockRequestHandleScope.fastlegeRestMockResponse(request: HttpRequestData): HttpResponseData {
    val personident = request.headers[NAV_PERSONIDENT_HEADER]
    val isVikarRequest = request.url.encodedPath.contains(FASTLEGEVIKAR_SYSTEM_PATH)
    return if (isVikarRequest) {
        when (personident) {
            UserConstants.ARBEIDSTAKER_MED_VIKARFASTLEGE.value -> respondOk(
                generateFastlegeResponse(UserConstants.HERID),
            )

            else -> respondError(HttpStatusCode.NotFound)
        }
    } else {
        when (personident) {
            UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value -> respondError(HttpStatusCode.NotFound)
            UserConstants.ARBEIDSTAKER_MED_VIKARFASTLEGE.value -> respondError(HttpStatusCode.NotFound)
            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET.value -> respondOk(
                generateFastlegeResponse(null)
            )

            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO.value -> respondOk(
                generateFastlegeResponse(UserConstants.HERID_UTEN_PARTNERINFO)
            )

            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO.value -> respondOk(
                generateFastlegeResponse(UserConstants.HERID_MED_FLERE_PARTNERINFO)
            )

            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID.value -> respondOk(
                generateFastlegeResponse(null, null, null)
            )

            else -> respondOk(
                generateFastlegeResponse(UserConstants.HERID)
            )
        }
    }
}
