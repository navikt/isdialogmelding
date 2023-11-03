package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

fun MockRequestHandleScope.tilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    return when (request.headers[NAV_PERSONIDENT_HEADER]) {
        ARBEIDSTAKER_VEILEDER_NO_ACCESS.value -> respondOk(Tilgang(erGodkjent = false))
        else -> respondOk(Tilgang(erGodkjent = true))
    }
}
