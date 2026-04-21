package no.nav.syfo.client

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.azuread.AzureAdToken
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.mocks.respondOk
import no.nav.syfo.testhelper.testEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VeilederTilgangskontrollClientTest {
    private val token = "token"
    private val oboToken = "obo-token"
    private val callId = "call-id"
    private val personident = UserConstants.ARBEIDSTAKER_FNR
    private val azureAdClient = mockk<AzureAdClient>()
    private val environment = testEnvironment()

    @BeforeEach
    fun setup() {
        coEvery {
            azureAdClient.getOnBehalfOfToken(any(), any())
        } returns AzureAdToken(
            accessToken = oboToken,
            expires = LocalDateTime.now().plusHours(1),
        )
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `hasAccess and hasWriteAccess returns false when tilgang is not approved`() {
        val client = createMockClientForResponse(Tilgang(erGodkjent = false, fullTilgang = true))

        runBlocking {
            assertFalse(client.hasAccess(callId, personident, token))
            assertFalse(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess and hasWriteAccess returns true when approved and user has fullTilgang`() {
        val client = createMockClientForResponse(Tilgang(erGodkjent = true, fullTilgang = true))

        runBlocking {
            assertTrue(client.hasAccess(callId, personident, token))
            assertTrue(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess returns true and hasWriteAccess returns false when approved but user does not have fullTilgang`() {
        val client = createMockClientForResponse(Tilgang(erGodkjent = true, fullTilgang = false))

        runBlocking {
            assertTrue(client.hasAccess(callId, personident, token))
            assertFalse(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess and hasWriteAccess returns false on unexpected response`() {
        val client = createMockClientForResponse(status = HttpStatusCode.InternalServerError)

        runBlocking {
            assertFalse(client.hasAccess(callId, personident, token))
            assertFalse(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess and hasWriteAccess returns false when access is Forbidden`() {
        val client = createMockClientForResponse(status = HttpStatusCode.Forbidden)

        runBlocking {
            assertFalse(client.hasAccess(callId, personident, token))
            assertFalse(client.hasWriteAccess(callId, personident, token))
        }
    }

    @Test
    fun `hasAccess throws when OBO token request fails`() {
        coEvery {
            azureAdClient.getOnBehalfOfToken(any(), any())
        } returns null

        val client = createMockClientForResponse()

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                client.hasAccess(callId, personident, token)
            }
        }
    }

    private fun createMockClientForResponse(
        tilgang: Tilgang = Tilgang(erGodkjent = true),
        status: HttpStatusCode = HttpStatusCode.OK,
    ): VeilederTilgangskontrollClient {
        val httpClient = HttpClient(MockEngine) {
            commonConfig()
            engine {
                addHandler {
                    if (status == HttpStatusCode.OK) {
                        respondOk(tilgang)
                    } else {
                        respond("error", status, headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()))
                    }
                }
            }
        }

        return VeilederTilgangskontrollClient(
            azureAdClient = azureAdClient,
            istilgangskontrollClientId = environment.istilgangskontrollClientId,
            tilgangskontrollBaseUrl = environment.istilgangskontrollUrl,
            httpClient = httpClient,
        )
    }
}
