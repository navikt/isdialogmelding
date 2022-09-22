package no.nav.syfo.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import no.nav.syfo.api.sendOppfolgingsplanPath
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class OppfolgingsplanSystemApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()
    val mqSender = mockk<MQSender>()
    justRun { mqSender.sendMessageToEmottak(any()) }

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val validToken = generateJWTIdporten(
            audience = externalMockEnvironment.environment.aadAppClient,
            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
            issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        )
        application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
            mqSender = mqSender,
        )

        afterEachTest {
            database.dropData()
            clearMocks(mqSender)
            justRun { mqSender.sendMessageToEmottak(any()) }
        }

        describe(OppfolgingsplanSystemApiSpek::class.java.simpleName) {
            describe("Send oppfolgingsplan for person") {
                val url = sendOppfolgingsplanPath
                describe("Happy path") {
                    it("Should send oppfolgingsplan") {

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
                    it("should return error for arbeidstaker with no fastlege") {
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
                    it("should return status BadRequest if  no ClientId is supplied") {
                        val tokenNoClientId = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.aadAppClient,
                            clientId = null,
                            issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                        )
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenNoClientId))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(generateRSOppfolgingsplan()))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        }
                    }

                    it("should return status Forbiddden if unauthorized ClientId is supplied") {
                        val tokenUnauthorizedClientId = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.aadAppClient,
                            clientId = "app",
                            issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                        )

                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenUnauthorizedClientId))
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
