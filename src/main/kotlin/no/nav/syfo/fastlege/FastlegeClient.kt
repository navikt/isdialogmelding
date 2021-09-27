package no.nav.syfo.fastlege

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class FastlegeClient(
    private val azureAdClient: AzureAdClient,
    private val fastlegeRestClientId: String,
    fastlegeRestUrl: String
) {

    private val httpClient = httpClientDefault()
    private val finnFastlegeUrl: String = "$fastlegeRestUrl$FASTLEGE_PATH"

    suspend fun fastlege(
        personIdentNumber: PersonIdentNumber,
        token: String,
        callId: String,
    ): FastlegeResponse? {
        val oboToken =
            azureAdClient.getOnBehalfOfToken(scopeClientId = fastlegeRestClientId, token = token)?.accessToken
                ?: throw RuntimeException("Failed to request fastlege from fastlegerest: Failed to get OBO token")
        try {
            val response = httpClient.get<FastlegeResponse>(finnFastlegeUrl) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personIdentNumber.value)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_FASTLEGEREST_FASTLEGE_SUCCESS.increment()
            return response
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e.response, e.message, callId)
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e.response, e.message, callId)
        }

        return null
    }

    private fun handleUnexpectedResponseException(response: HttpResponse, message: String?, callId: String) {
        log.error(
            "Error while requesting Response from fastlegerest {}, {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            StructuredArguments.keyValue("message", message),
            StructuredArguments.keyValue("callId", callId),
        )
        COUNT_CALL_FASTLEGEREST_FASTLEGE_FAIL.increment()
    }

    companion object {
        const val FASTLEGE_PATH = "/api/v2/fastlege"
        private val log = LoggerFactory.getLogger(FastlegeClient::class.java)
    }
}
