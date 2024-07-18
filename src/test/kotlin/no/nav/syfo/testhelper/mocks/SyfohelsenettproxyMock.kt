package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.syfohelsenettproxy.Godkjenning
import no.nav.syfo.client.syfohelsenettproxy.HelsenettProxyBehandler
import no.nav.syfo.client.syfohelsenettproxy.Kode
import no.nav.syfo.testhelper.UserConstants

fun MockRequestHandleScope.syfohelsenettproxyResponse(request: HttpRequestData): HttpResponseData {
    val hprId = request.headers["hprNummer"]!!.toInt()
    return respondOk(
        HelsenettProxyBehandler(
            godkjenninger = listOf(Godkjenning(Kode(true, 1, "LE"))),
            fnr = if (hprId == UserConstants.HPRID) UserConstants.FASTLEGE_FNR.value else UserConstants.FASTLEGE_ANNEN_FNR.value,
            hprNummer = hprId,
            fornavn = UserConstants.BEHANDLER_FORNAVN,
            mellomnavn = null,
            etternavn = UserConstants.BEHANDLER_ETTERNAVN,
        )
    )
}
