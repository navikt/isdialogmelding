package no.nav.syfo.client.btsys

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.Personident
import java.io.IOException

class LegeSuspensjonClient(
    private val azureAdClient: AzureAdClient,
    private val endpointUrl: String,
    private val endpointClientId: String,
    private val httpClient: HttpClient = httpClientDefault(),
) {
    suspend fun sjekkSuspensjon(
        behandlerId: Personident,
    ): Suspendert {
        val token = azureAdClient.getSystemToken(endpointClientId)
            ?: throw RuntimeException("Failed to sjekk suspensjon: No token was found")

        val httpResponse: HttpResponse = httpClient.get("$endpointUrl/api/v1/suspensjon/status") {
            accept(ContentType.Application.Json)
            header("Nav-Personident", behandlerId.value)
            header("Authorization", "Bearer ${token.accessToken}")
        }
        if (httpResponse.status != HttpStatusCode.OK) {
            throw IOException("Btsys svarte med uventet kode ${httpResponse.status}")
        }
        return httpResponse.call.response.body()
    }
}

data class Suspendert(val suspendert: Boolean)
