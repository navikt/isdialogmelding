package no.nav.syfo.behandler.partnerinfo

import io.ktor.client.features.*
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
    ): PartnerinfoResponse? {
        val oboToken =
            azureAdClient.getOnBehalfOfToken(scopeClientId = syfoPartnerinfoClientId, token = token)?.accessToken
                ?: throw RuntimeException("Failed to request partnerinfo from syfopartnerinfo: Failed to get OBO token")
        try {
            val response = httpClient.get<List<PartnerinfoResponse>>(partnerinfoBehandlerUrl) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                parameter(HERID_PARAM, herId)
            }
            COUNT_CALL_PARTNERINFO_SUCCESS.increment()
            return response.firstOrNull()
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
