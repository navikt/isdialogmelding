package no.nav.syfo.behandler.partnerinfo

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class PartnerinfoClient(
    private val azureAdClient: AzureAdClient,
    private val syfoPartnerinfoClientId: String,
    syfoPartnerinfoUrl: String,
) {

    private val httpClient = httpClientDefault()
    private val partnerinfoBehandlerUrl: String = "$syfoPartnerinfoUrl$BEHANDLER_PATH"

    suspend fun partnerinfo(
        herId: String,
        token: String,
        callId: String,
        systemRequest: Boolean = false,
    ): PartnerinfoResponse? {
        val newToken = if (systemRequest) {
            azureAdClient.getSystemToken(
                scopeClientId = syfoPartnerinfoClientId,
            )?.accessToken
                ?: throw RuntimeException("Failed to request partnerinfo from syfopartnerinfo: Failed to get System token")
        } else {
            azureAdClient.getOnBehalfOfToken(
                scopeClientId = syfoPartnerinfoClientId,
                token = token,
            )?.accessToken
                ?: throw RuntimeException("Failed to request partnerinfo from syfopartnerinfo: Failed to get OBO token")
        }

        try {
            val response = httpClient.get(partnerinfoBehandlerUrl) {
                header(HttpHeaders.Authorization, bearerHeader(newToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                parameter(HERID_PARAM, herId)
            }.body<List<PartnerinfoResponse>>()
            COUNT_CALL_PARTNERINFO_SUCCESS.increment()

            if (response.isEmpty()) {
                log.warn("Response from syfopartnerinfo for herId $herId is empty")
                COUNT_CALL_PARTNERINFO_EMPTY_RESPONSE.increment()
            }
            if (response.size > 1) {
                log.warn("Response from syfopartnerinfo for herId $herId contains more than one partnerId: ${response.map { it.partnerId }}")
                COUNT_CALL_PARTNERINFO_MULTIPLE_RESPONSE.increment()
            }

            return response.maxByOrNull { it.partnerId }
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e.response, e.message, callId)
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e.response, e.message, callId)
        }

        return null
    }

    private fun handleUnexpectedResponseException(response: HttpResponse, message: String?, callId: String) {
        log.error(
            "Error while requesting Response from syfopartnerinfo {}, {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            StructuredArguments.keyValue("message", message),
            StructuredArguments.keyValue("callId", callId),
        )
        COUNT_CALL_PARTNERINFO_FAIL.increment()
    }

    companion object {
        const val BEHANDLER_PATH = "/api/v2/behandler"
        const val HERID_PARAM = "herid"
        private val log = LoggerFactory.getLogger(PartnerinfoClient::class.java)
    }
}
