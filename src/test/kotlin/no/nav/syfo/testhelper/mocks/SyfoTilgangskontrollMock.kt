package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.application.api.authentication.getNAVIdentFromToken
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import no.nav.syfo.testhelper.UserConstants.VEILEDER_IDENT_NO_WRITE_ACCESS
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

private fun HttpRequestData.navIdent(): String? =
    headers[HttpHeaders.Authorization]
        ?.removePrefix("Bearer ")
        ?.let { getNAVIdentFromToken(it) }

fun MockRequestHandleScope.tilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    val erGodkjent = request.headers[NAV_PERSONIDENT_HEADER] != ARBEIDSTAKER_VEILEDER_NO_ACCESS.value
    val fullTilgang = request.navIdent() != VEILEDER_IDENT_NO_WRITE_ACCESS
    return respondOk(Tilgang(erGodkjent = erGodkjent, fullTilgang = fullTilgang))
}
