package no.nav.syfo.client.veiledertilgang

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

class VeilederTilgangskontrollClient(
    private val azureAdClient: AzureAdClient,
    private val istilgangskontrollClientId: String,
    tilgangskontrollBaseUrl: String,
    private val httpClient: HttpClient = httpClientDefault(),
) {
    private val tilgangskontrollInnbyggerUrl = "$tilgangskontrollBaseUrl$TILGANGSKONTROLL_INNBYGGER_PATH"

    suspend fun hasInnbyggerAccess(
        callId: String,
        personident: Personident,
        token: String,
    ): Boolean {
        val onBehalfOfToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = istilgangskontrollClientId,
            token = token,
        )?.accessToken ?: throw RuntimeException("Failed to request access to innbygger: Failed to get OBO token")

        return try {
            val response = httpClient.get(tilgangskontrollInnbyggerUrl) {
                header(HttpHeaders.Authorization, bearerHeader(onBehalfOfToken))
                header(NAV_PERSONIDENT_HEADER, personident.value)
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_TILGANGSKONTROLL_PERSON_SUCCESS.increment()
            response.body<Tilgang>().erGodkjent
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                COUNT_CALL_TILGANGSKONTROLL_PERSON_FORBIDDEN.increment()
            } else {
                handleUnexpectedResponseException(e.response, callId)
            }
            false
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e.response, callId)
            false
        }
    }

    private fun handleUnexpectedResponseException(
        response: HttpResponse,
        callId: String,
    ) {
        log.error(
            "Error while requesting access to innbygger from istilgangskontroll with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId)
        )
        COUNT_CALL_TILGANGSKONTROLL_PERSON_FAIL.increment()
    }

    companion object {
        private val log = LoggerFactory.getLogger(VeilederTilgangskontrollClient::class.java)

        const val TILGANGSKONTROLL_INNBYGGER_PATH = "/api/tilgang/navident/innbygger"
    }
}
