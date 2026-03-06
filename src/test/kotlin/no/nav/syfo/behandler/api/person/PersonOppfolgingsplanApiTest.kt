package no.nav.syfo.behandler.api.person

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.mockk
import io.mockk.verify
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

class PersonOppfolgingsplanApiTest {
    private val mqSender = mockk<MQSender>(relaxed = true)
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val url = personApiOppfolgingsplanPath

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {
        @Test
        fun `Skal lagre bestilling for oppfølgingsplan`() {
            val validToken = generateJWTIdporten(
                audience = externalMockEnvironment.environment.idportenTokenXClientId,
                clientId = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
                issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                pid = UserConstants.ARBEIDSTAKER_FNR.value,
            )
            val rsOppfolgingsplan = generateRSOppfolgingsplan()
            assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
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
                assertEquals(UserConstants.ARBEIDSTAKER_FNR.value, storedBestilling.arbeidstakerPersonident)
                assertNull(storedBestilling.sendt)
            }
        }

        @Test
        fun `Skal lagre bestilling for innsendt oppfølgingsplan fra nærmeste leder`() {
            val validToken = generateJWTIdporten(
                audience = externalMockEnvironment.environment.idportenTokenXClientId,
                clientId = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
                issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                pid = UserConstants.NARMESTELEDER_FNR.value,
            )
            val rsOppfolgingsplan = generateRSOppfolgingsplan()
            assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())

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
    }

    @Nested
    @DisplayName("Unhappy paths")
    inner class UnhappyPaths {
        @Test
        fun `should return error for arbeidstaker with no fastlege`() {
            val validToken = generateJWTIdporten(
                audience = externalMockEnvironment.environment.idportenTokenXClientId,
                clientId = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
                issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                pid = UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value,
            )

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
        fun `should return status Unauthorized if no token is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan())
                }

                assertEquals(HttpStatusCode.Unauthorized, response.status)
                assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            }
        }

        @Test
        fun `should return status BadRequest if no PID is supplied`() {
            val tokenNoPid = generateJWTIdporten(
                audience = externalMockEnvironment.environment.idportenTokenXClientId,
                clientId = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
                issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                pid = null,
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(tokenNoPid)
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan())
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            }
        }

        @Test
        fun `should return status BadRequest if invalid Personident in PID is supplied`() {
            val tokenInvalidPid = generateJWTIdporten(
                audience = externalMockEnvironment.environment.idportenTokenXClientId,
                clientId = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
                issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                pid = UserConstants.ARBEIDSTAKER_FNR.value.drop(1),
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(tokenInvalidPid)
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan())
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                assertNull(database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull())
            }
        }

        @Test
        fun `should return status BadRequest if valid PID with and no ClientId is supplied`() {
            val tokenValidPidNoClientId = generateJWTIdporten(
                audience = externalMockEnvironment.environment.idportenTokenXClientId,
                clientId = null,
                issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                pid = UserConstants.ARBEIDSTAKER_FNR.value,
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(tokenValidPidNoClientId)
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan())
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
                verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
            }
        }

        @Test
        fun `should return status Forbiddden if valid PID with and unauthorized ClientId is supplied`() {
            val tokenValidPidUnauthorizedClientId = generateJWTIdporten(
                audience = externalMockEnvironment.environment.idportenTokenXClientId,
                clientId = "app",
                issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                pid = UserConstants.ARBEIDSTAKER_FNR.value,
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(tokenValidPidUnauthorizedClientId)
                    contentType(ContentType.Application.Json)
                    setBody(generateRSOppfolgingsplan())
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
                verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
            }
        }
    }
}
