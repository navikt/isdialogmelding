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
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class OppfolgingsplanSystemApiSpek : Spek({
    val mqSender = mockk<MQSender>(relaxed = true)
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val validToken = generateJWTSystem(
        audience = externalMockEnvironment.environment.aadAppClient,
        azp = externalMockEnvironment.environment.syfooppfolgingsplanserviceClientId,
        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
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
                it("Skal lagre bestilling for oppfølgingsplan når bare vikar for fastlege finnes") {
                    database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
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

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val storedBestilling = database.getDialogmeldingToBehandlerBestillingNotSendt().first()
                        storedBestilling.type shouldBeEqualTo DialogmeldingType.OPPFOLGINGSPLAN.name
                        storedBestilling.arbeidstakerPersonident shouldBeEqualTo rsOppfolgingsplan.sykmeldtFnr
                    }
                }
            }
            describe("Unhappy paths") {
                it("should return error for arbeidstaker with no fastlege or vikar") {
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
                            setBody(generateRSOppfolgingsplan(UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR))
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Unauthorized
                        database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                    }
                }
                it("should return status BadRequest if no ClientId is supplied") {
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

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                        database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                    }
                }

                it("should return status Forbidden if unauthorized ClientId is supplied") {
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

                        response.status shouldBeEqualTo HttpStatusCode.Forbidden
                        database.getDialogmeldingToBehandlerBestillingNotSendt().firstOrNull() shouldBe null
                    }
                }
            }
        }
    }
})
