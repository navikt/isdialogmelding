package no.nav.syfo.application.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import no.nav.syfo.api.sendOppfolgingsplanPath
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.database.getDialogmeldingToBehandlerBestillingNotSendt
import no.nav.syfo.behandler.domain.DialogmeldingType
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class OppfolgingsplanSystemApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()
    val mqSender = mockk<MQSender>(relaxed = true)

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val validToken = generateJWTSystem(
            audience = externalMockEnvironment.environment.aadAppClient,
            azp = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
            issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        )
        application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
        )

        afterEachTest {
            database.dropData()
        }

        describe(OppfolgingsplanSystemApiSpek::class.java.simpleName) {
            describe("Send oppfolgingsplan for person") {
                val url = sendOppfolgingsplanPath
                describe("Happy path") {
                    it("Skal lagre bestilling for oppfølgingsplan (men ikke sende)") {
                        database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                        val rsOppfolgingsplan = generateRSOppfolgingsplan()
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(rsOppfolgingsplan))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                            val storedBestilling = database.getDialogmeldingToBehandlerBestillingNotSendt().first()
                            storedBestilling.type shouldBeEqualTo DialogmeldingType.OPPFOLGINGSPLAN.name
                            storedBestilling.arbeidstakerPersonident shouldBeEqualTo rsOppfolgingsplan.sykmeldtFnr
                            storedBestilling.sendt shouldBe null
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
                            response.status() shouldBeEqualTo HttpStatusCode.NotFound
                            database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
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
                            database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
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
                            database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                        }
                    }

                    it("should return status Forbidden if unauthorized ClientId is supplied") {
                        val tokenUnauthorizedClientId = generateJWTSystem(
                            audience = externalMockEnvironment.environment.aadAppClient,
                            azp = "app",
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
                            database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                        }
                    }
                }
            }
        }
    }
})
