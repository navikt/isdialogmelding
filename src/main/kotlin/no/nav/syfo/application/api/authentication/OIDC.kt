package no.nav.syfo.application.api.authentication

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.httpClientProxy

fun getWellKnown(wellKnownUrl: String) =
    runBlocking {
        httpClientProxy().use { client ->
            client.get(wellKnownUrl).body<WellKnown>()
        }
    }

@JsonIgnoreProperties(ignoreUnknown = true)
data class WellKnown(
    val authorization_endpoint: String,
    val token_endpoint: String,
    val jwks_uri: String,
    val issuer: String
)
