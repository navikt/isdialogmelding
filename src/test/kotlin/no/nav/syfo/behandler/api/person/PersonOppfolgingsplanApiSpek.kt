package no.nav.syfo.behandler.api.person

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class PersonOppfolgingsplanApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()
    val mqSender = mockk<MQSender>()
    justRun { mqSender.sendMessageToEmottak(any()) }

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
            mqSender = mqSender,
        )

        afterEachTest {
            database.dropData()
            clearMocks(mqSender)
            justRun { mqSender.sendMessageToEmottak(any()) }
        }

        describe(PersonOppfolgingsplanApiSpek::class.java.simpleName) {
            describe("Send oppfolgingsplan for person") {
                val url = "$personApiOppfolgingsplanPath"
                describe("Happy path") {
                    it("Should send oppfolgingsplan") {
                        val validToken = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_FNR.value,
                        )

                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan()))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }
                }
                describe("Unhappy paths") {
                    it("Should not send oppfolgingsplan for other arbeidstaker") {
                        val validToken = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_FNR.value,
                        )

                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan(UserConstants.ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO)))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }
                    it("should return error for arbeidstaker with no fastlege") {
                        val validToken = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value,
                        )
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan(UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR)))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }

                    it("should return status Unauthorized if no token is supplied") {
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan()))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }

                    it("should return status BadRequest if no PID is supplied") {
                        val tokenNoPid = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = null,
                        )

                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenNoPid))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan()))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }

                    it("should return status BadRequest if invalid Personident in PID is supplied") {
                        val tokenInvalidPid = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_FNR.value.drop(1),
                        )

                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenInvalidPid))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan()))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }

                    it("should return status BadRequest if valid PID with and no ClientId is supplied") {
                        val tokenValidPidNoClientId = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = null,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_FNR.value,
                        )

                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenValidPidNoClientId))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan()))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }

                    it("should return status Forbiddden if valid PID with and unauthorized ClientId is supplied") {
                        val tokenValidPidUnauthorizedClientId = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = "app",
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_FNR.value,
                        )

                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenValidPidUnauthorizedClientId))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan()))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Forbidden
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }
                }
            }
        }
    }
})
