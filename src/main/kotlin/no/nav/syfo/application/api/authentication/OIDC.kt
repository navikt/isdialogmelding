package no.nav.syfo.application.api.authentication

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.httpClientProxy

private val httpClient = httpClientProxy()

fun getWellKnown(wellKnownUrl: String) =
    runBlocking {
        httpClient.get(wellKnownUrl).body<WellKnown>()
    }

@JsonIgnoreProperties(ignoreUnknown = true)
data class WellKnown(
    val authorization_endpoint: String,
    val token_endpoint: String,
    val jwks_uri: String,
    val issuer: String
)
