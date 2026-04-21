package no.nav.syfo.client.veiledertilgang

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class VeilederTilgangskontrollClient(
    private val azureAdClient: AzureAdClient,
    private val istilgangskontrollClientId: String,
    tilgangskontrollBaseUrl: String,
    private val httpClient: HttpClient = httpClientDefault(),
) {
    private val tilgangskontrollPersonUrl = "$tilgangskontrollBaseUrl$TILGANGSKONTROLL_PERSON_PATH"

    suspend fun hasAccess(callId: String, personident: Personident, token: String): Boolean =
        getTilgang(callId, personident, token)?.erGodkjent ?: false

    suspend fun hasWriteAccess(callId: String, personident: Personident, token: String): Boolean =
        getTilgang(callId, personident, token)?.let {
            it.erGodkjent && it.fullTilgang
        } ?: false

    private suspend fun getTilgang(callId: String, personident: Personident, token: String): Tilgang? {
        val onBehalfOfToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = istilgangskontrollClientId,
            token = token,
        )?.accessToken ?: throw RuntimeException("Failed to request access to Person: Failed to get OBO token")

        return try {
            val response = httpClient.get(tilgangskontrollPersonUrl) {
                header(HttpHeaders.Authorization, bearerHeader(onBehalfOfToken))
                header(NAV_PERSONIDENT_HEADER, personident.value)
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_TILGANGSKONTROLL_PERSON_SUCCESS.increment()
            response.body<Tilgang>()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                COUNT_CALL_TILGANGSKONTROLL_PERSON_FORBIDDEN.increment()
            } else {
                log.error("Error while requesting access to person from istilgangskontroll with statuscode: ${e.response.status.value}, callId: $callId")
                COUNT_CALL_TILGANGSKONTROLL_PERSON_FAIL.increment()
            }
            null
        } catch (e: ServerResponseException) {
            log.error("Error while requesting access to person from istilgangskontroll with statuscode: ${e.response.status.value}, callId: $callId")
            COUNT_CALL_TILGANGSKONTROLL_PERSON_FAIL.increment()
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(VeilederTilgangskontrollClient::class.java)

        const val TILGANGSKONTROLL_PERSON_PATH = "/api/tilgang/navident/person"
    }
}
