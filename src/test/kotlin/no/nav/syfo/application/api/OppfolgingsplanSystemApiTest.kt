package no.nav.syfo.application.api

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.api.sendOppfolgingsplanPath
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.dialogmelding.bestilling.database.getDialogmeldingToBehandlerBestillingNotSendt
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingType
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateRSOppfolgingsplan
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OppfolgingsplanSystemApiTest {
    private val mqSender = mockk<MQSender>(relaxed = true)
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val validToken = generateJWTSystem(
        audience = externalMockEnvironment.environment.aadAppClient,
        azp = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
    )
    private val url = sendOppfolgingsplanPath

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {
        @Test
        fun `Skal lagre bestilling for oppfølgingsplan (men ikke sende)`() {
            assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            val rsOppfolgingsplan = generateRSOppfolgingsplan()
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(rsOppfolgingsplan)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                val storedBestilling = database.getDialogmeldingToBehandlerBestillingNotSendt().first()
                assertEquals(DialogmeldingType.OPPFOLGINGSPLAN.name, storedBestilling.type)
                assertEquals(rsOppfolgingsplan.sykmeldtFnr, storedBestilling.arbeidstakerPersonident)
                assertNull(storedBestilling.sendt)
            }
        }

        @Test
        fun `Skal lagre bestilling for oppfølgingsplan når bare vikar for fastlege finnes`() {
            assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            val rsOppfolgingsplan = generateRSOppfolgingsplan(
                arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_MED_VIKARFASTLEGE,
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(rsOppfolgingsplan)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val storedBestilling = database.getDialogmeldingToBehandlerBestillingNotSendt().first()
                assertEquals(DialogmeldingType.OPPFOLGINGSPLAN.name, storedBestilling.type)
                assertEquals(rsOppfolgingsplan.sykmeldtFnr, storedBestilling.arbeidstakerPersonident)
            }
        }
    }

    @Nested
    @DisplayName("Unhappy paths")
    inner class UnhappyPaths {
        @Test
        fun `should return error for arbeidstaker with no fastlege or vikar`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan(UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR))
                }

                assertEquals(HttpStatusCode.NotFound, response.status)
                assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            }
        }

        @Test
        fun `should return InternalServerError when storing oppfolgingsplan fails`() {
            testApplication {
                val client = setupApiAndClient(database = TestDatabaseNotResponding())
                val response = client.post(url) {
                    bearerAuth(validToken)
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan())
                }

                assertEquals(HttpStatusCode.InternalServerError, response.status)
            }
        }

        @Test
        fun `should return status Unauthorized if no token is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan(UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR))
                }

                assertEquals(HttpStatusCode.Unauthorized, response.status)
                assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            }
        }

        @Test
        fun `should return status BadRequest if no ClientId is supplied`() {
            val tokenNoClientId = generateJWTIdporten(
                audience = externalMockEnvironment.environment.aadAppClient,
                clientId = null,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
            )
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(tokenNoClientId)
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan())
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            }
        }

        @Test
        fun `should return status Forbidden if unauthorized ClientId is supplied`() {
            val tokenUnauthorizedClientId = generateJWTSystem(
                audience = externalMockEnvironment.environment.aadAppClient,
                azp = "app",
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
            )
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(tokenUnauthorizedClientId)
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan())
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
                assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            }
        }
    }
}
