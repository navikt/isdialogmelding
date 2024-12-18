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
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class PersonOppfolgingsplanApiSpek : Spek({
    val mqSender = mockk<MQSender>(relaxed = true)
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

    afterEachTest {
        database.dropData()
    }

    describe(PersonOppfolgingsplanApiSpek::class.java.simpleName) {
        describe("Send oppfolgingsplan for person") {
            val url = personApiOppfolgingsplanPath
            describe("Happy path") {
                it("Skal lagre bestilling for oppfølgingsplan") {
                    val validToken = generateJWTIdporten(
                        audience = externalMockEnvironment.environment.idportenTokenXClientId,
                        clientId = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
                        issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                        pid = UserConstants.ARBEIDSTAKER_FNR.value,
                    )
                    val rsOppfolgingsplan = generateRSOppfolgingsplan()
                    database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(url) {
                            bearerAuth(validToken)
                            contentType(ContentType.Application.Json)
                            setBody(rsOppfolgingsplan)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                        val storedBestilling = database.getDialogmeldingToBehandlerBestillingNotSendt().first()
                        storedBestilling.type shouldBeEqualTo DialogmeldingType.OPPFOLGINGSPLAN.name
                        storedBestilling.arbeidstakerPersonident shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR.value
                        storedBestilling.sendt shouldBe null
                    }
                }
                it("Skal lagre bestilling for innsendt oppfølgingsplan fra nærmeste leder") {
                    val validToken = generateJWTIdporten(
                        audience = externalMockEnvironment.environment.idportenTokenXClientId,
                        clientId = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
                        issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                        pid = UserConstants.NARMESTELEDER_FNR.value,
                    )
                    val rsOppfolgingsplan = generateRSOppfolgingsplan()
                    database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(url) {
                            bearerAuth(validToken)
                            contentType(ContentType.Application.Json)
                            setBody(rsOppfolgingsplan)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
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

                        response.status shouldBeEqualTo HttpStatusCode.NotFound
                        database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                    }
                }

                it("should return status Unauthorized if no token is supplied") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(url) {
                            contentType(ContentType.Application.Json)
                            setBody(generateRSOppfolgingsplan())
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Unauthorized
                        database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                    }
                }

                it("should return status BadRequest if no PID is supplied") {
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

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                        database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                    }
                }

                it("should return status BadRequest if invalid Personident in PID is supplied") {
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

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                        database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                    }
                }

                it("should return status BadRequest if valid PID with and no ClientId is supplied") {
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

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(url) {
                            bearerAuth(tokenValidPidUnauthorizedClientId)
                            contentType(ContentType.Application.Json)
                            setBody(generateRSOppfolgingsplan())
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Forbidden
                        verify(exactly = 0) { mqSender.sendMessageToEmottak(any()) }
                    }
                }
            }
        }
    }
})
