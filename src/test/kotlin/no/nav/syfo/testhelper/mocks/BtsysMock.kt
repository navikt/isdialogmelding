package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.btsys.Suspendert
import no.nav.syfo.client.btsys.SuspensjonSoekRequest
import no.nav.syfo.testhelper.UserConstants

suspend fun MockRequestHandleScope.btsysMockResponse(request: HttpRequestData): HttpResponseData {
    val requestBody = request.receiveBody<SuspensjonSoekRequest>()
    return respondOk(Suspendert(UserConstants.FASTLEGE_FNR_SUSPENDERT.value == requestBody.personident))
}
