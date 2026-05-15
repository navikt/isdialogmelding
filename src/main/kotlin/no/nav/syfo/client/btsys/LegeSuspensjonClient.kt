package no.nav.syfo.client.btsys

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import java.io.IOException
import java.util.*

const val APPLICATION_NAME = "isdialogmelding"

class LegeSuspensjonClient(
    private val azureAdClient: AzureAdClient,
    private val endpointUrl: String,
    private val endpointClientId: String,
    private val httpClient: HttpClient = httpClientDefault(),
) {
    suspend fun sjekkSuspensjon(
        behandlerPersonident: Personident,
    ): Suspendert {
        val token = azureAdClient.getSystemToken(endpointClientId)
            ?: throw RuntimeException("Failed to sjekk suspensjon: No token was found")

        val httpResponse: HttpResponse = httpClient.post("$endpointUrl/api/v1/suspensjon/soek") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            header("Nav-Consumer-Id", APPLICATION_NAME)
            header(NAV_CALL_ID_HEADER, UUID.randomUUID().toString())
            header("Authorization", bearerHeader(token.accessToken))
            setBody(SuspensjonSoekRequest(personident = behandlerPersonident.value))
        }
        if (httpResponse.status != HttpStatusCode.OK) {
            throw IOException("Btsys svarte med uventet kode ${httpResponse.status}")
        }
        return httpResponse.call.response.body()
    }
}

data class SuspensjonSoekRequest(val personident: String)

data class Suspendert(val suspendert: Boolean)
