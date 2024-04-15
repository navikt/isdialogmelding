package no.nav.syfo.behandler.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID

class BehandlerSystemApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        application.testApiModule(externalMockEnvironment = externalMockEnvironment)

        afterEachTest {
            database.dropData()
        }

        describe(BehandlerSystemApiSpek::class.java.simpleName) {

            val validToken = generateJWTSystem(
                audience = externalMockEnvironment.environment.aadAppClient,
                azp = isfrisktilarbeidClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
            )

            describe("Get behandler for behandlerRef") {
                val behandlerRef = UUID.randomUUID()
                val url = "$behandlerSystemApiPath/$behandlerRef"
                val behandler = generateBehandler(
                    behandlerRef = behandlerRef,
                    partnerId = PARTNERID,
                )
                describe("Happy path") {
                    it("should return behandler for behandlerRef") {
                        database.createBehandlerForArbeidstaker(
                            behandler = behandler,
                            arbeidstakerPersonident = ARBEIDSTAKER_FNR,
                        )
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            val behandlerDTO =
                                objectMapper.readValue<BehandlerDTO>(response.content!!)
                            behandlerDTO.behandlerRef shouldBeEqualTo behandlerRef.toString()
                            behandlerDTO.fornavn shouldBeEqualTo "Dana"
                        }
                    }
                }
                describe("Unhappy path") {
                    it("should return status NotFound for non-matching behandlerRef") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NotFound
                        }
                    }
                    it("should return status Unauthorized if no token is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {}
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
                        }
                    }
                    it("should return status BadRequest if invalid behandlerRef") {
                        with(
                            handleRequest(HttpMethod.Get, "$behandlerPath/123abc") {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }
                    it("should return status Forbidden if wrong consumer azp") {
                        val invalidValidToken = generateJWTSystem(
                            audience = externalMockEnvironment.environment.aadAppClient,
                            azp = testSyfooppfolgingsplanserviceClientId,
                            issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                        )

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(invalidValidToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Forbidden
                        }
                    }
                }
            }
        }
    }
})
