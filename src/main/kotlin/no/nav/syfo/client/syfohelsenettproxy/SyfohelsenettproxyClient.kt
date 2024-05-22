package no.nav.syfo.client.syfohelsenettproxy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import org.slf4j.LoggerFactory
import java.io.IOException

class SyfohelsenettproxyClient(
    private val azureAdClient: AzureAdClient,
    private val endpointUrl: String,
    private val endpointClientId: String,
    private val httpClient: HttpClient = httpClientDefault(),
) {
    suspend fun finnBehandlerFraHpr(
        behandlerHpr: String,
    ): HelsenettProxyBehandler? {
        val accessToken = azureAdClient.getSystemToken(endpointClientId)?.accessToken
            ?: throw RuntimeException("Failed to send request to SyfohelsenettProxy: No token was found")

        return try {
            val response: HttpResponse = httpClient.get("$endpointUrl/api/v2/behandlerMedHprNummer") {
                accept(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                    append("hprNummer", behandlerHpr)
                }
            }
            response.body<HelsenettProxyBehandler>()
        } catch (exception: ResponseException) {
            when (exception.response.status) {
                BadRequest -> {
                    logger.error("BadRequest fra syfohelsenettproxy")
                    null
                }
                NotFound -> {
                    logger.warn("Behandler ikke funnet syfohelsenettproxy")
                    null
                }
                else -> {
                    logger.error("Feil fra syfohelsenettproxy")
                    throw IOException("Syfohelsenettproxy svarte med feilmelding")
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SyfohelsenettproxyClient::class.java)
    }
}

data class HelsenettProxyBehandler(
    val godkjenninger: List<Godkjenning>,
    val fnr: String?,
    val hprNummer: Int?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
) {
    fun getBehandlerKategori(): BehandlerKategori? {
        return if (godkjenninger.any { it.helsepersonellkategori?.aktiv == true && it.helsepersonellkategori?.verdi == BehandlerKategori.LEGE.kategoriKode }) {
            BehandlerKategori.LEGE
        } else {
            godkjenninger.firstOrNull { it.helsepersonellkategori?.aktiv == true }?.helsepersonellkategori?.let {
                BehandlerKategori.fromKategoriKode(it.verdi)
            }
        }
    }
}

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null
)

data class Kode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?
)
