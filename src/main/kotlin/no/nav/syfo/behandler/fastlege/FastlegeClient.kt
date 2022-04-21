package no.nav.syfo.behandler.fastlege

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class FastlegeClient(
    private val azureAdClient: AzureAdClient,
    private val fastlegeRestClientId: String,
    fastlegeRestUrl: String
) {
    private val httpClient = httpClientDefault()
    private val finnFastlegeUrl: String = "$fastlegeRestUrl$FASTLEGE_PATH"
    private val finnFastlegeSystemUrl: String = "$fastlegeRestUrl$FASTLEGE_SYSTEM_PATH"

    suspend fun fastlege(
        callId: String,
        personIdentNumber: PersonIdentNumber,
        systemRequest: Boolean = false,
        token: String,
    ): FastlegeResponse? {
        val newToken: String
        val url: String
        if (systemRequest) {
            newToken = azureAdClient.getSystemToken(
                scopeClientId = fastlegeRestClientId,
            )?.accessToken
                ?: throw RuntimeException("Failed to request fastlege from fastlegeres: Failed to get System token")
            url = finnFastlegeSystemUrl
        } else {
            newToken = azureAdClient.getOnBehalfOfToken(
                scopeClientId = fastlegeRestClientId,
                token = token,
            )?.accessToken
                ?: throw RuntimeException("Failed to request fastlege from fastlegeres Failed to get OBO token")
            url = finnFastlegeUrl
        }

        return fastlege(
            callId = callId,
            personIdentNumber = personIdentNumber,
            token = newToken,
            url = url,
        )
    }

    private suspend fun fastlege(
        callId: String,
        token: String,
        personIdentNumber: PersonIdentNumber,
        url: String,
    ): FastlegeResponse? {
        try {
            val response = httpClient.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(token))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personIdentNumber.value)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_FASTLEGEREST_FASTLEGE_SUCCESS.increment()
            return response.body()
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
        const val FASTLEGE_PATH = "/fastlegerest/api/v2/fastlege"
        const val FASTLEGE_SYSTEM_PATH = "/fastlegerest/api/system/v1/fastlege/aktiv/personident"
        private val log = LoggerFactory.getLogger(FastlegeClient::class.java)
    }
}
