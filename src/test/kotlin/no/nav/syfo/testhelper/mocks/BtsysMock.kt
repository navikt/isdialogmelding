package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.btsys.Suspendert
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

fun MockRequestHandleScope.btsysMockResponse(request: HttpRequestData): HttpResponseData {
    val personIdentHeader = request.headers.get(NAV_PERSONIDENT_HEADER)
    return respondOk(Suspendert(UserConstants.FASTLEGE_FNR_SUSPENDERT.value.equals(personIdentHeader)))
}
