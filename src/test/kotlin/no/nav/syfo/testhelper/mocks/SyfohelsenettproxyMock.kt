package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.syfohelsenettproxy.Godkjenning
import no.nav.syfo.client.syfohelsenettproxy.HelsenettProxyBehandler
import no.nav.syfo.client.syfohelsenettproxy.Kode
import no.nav.syfo.testhelper.UserConstants

fun MockRequestHandleScope.syfohelsenettproxyResponse(request: HttpRequestData): HttpResponseData {
    return respondOk(
        HelsenettProxyBehandler(
            godkjenninger = listOf(Godkjenning(Kode(true, 1, "LE"))),
            fnr = UserConstants.FASTLEGE_FNR.value,
            hprNummer = request.headers["hprNummer"]!!.toInt(),
            fornavn = UserConstants.BEHANDLER_FORNAVN,
            mellomnavn = null,
            etternavn = UserConstants.BEHANDLER_ETTERNAVN,
        )
    )
}
