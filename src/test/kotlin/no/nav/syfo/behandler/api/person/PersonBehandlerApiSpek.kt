package no.nav.syfo.behandler.api.person

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.behandler.database.getBehandlerDialogmeldingForArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerType
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class PersonBehandlerApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
        )

        afterEachTest {
            database.dropData()
        }

        describe(PersonBehandlerApiSpek::class.java.simpleName) {
            describe("Get list of BehandlerDialogmelding for PersonIdent") {
                val dtoClassName = PersonBehandlerDTO::class.java.simpleName

                val url = "$personApiBehandlerPath$personBehandlerSelfPath"
                describe("Happy path") {
                    it("should return list of $dtoClassName and store behandler if request is successful") {
                        val validToken = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_FNR.value,
                        )

                        val fastlegeResponse = generateFastlegeResponse()
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<PersonBehandlerDTO>>(response.content!!)
                            behandlerDialogmeldingList.size shouldBeEqualTo 1

                            val behandlerDialogmeldingForPersonList = database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_FNR,
                            )
                            behandlerDialogmeldingForPersonList.size shouldBeEqualTo 1

                            val behandlerDialogmeldingDTO = behandlerDialogmeldingList.first()
                            behandlerDialogmeldingDTO.fornavn shouldBeEqualTo fastlegeResponse.fornavn
                            behandlerDialogmeldingDTO.mellomnavn shouldBeEqualTo fastlegeResponse.mellomnavn
                            behandlerDialogmeldingDTO.etternavn shouldBeEqualTo fastlegeResponse.etternavn
                            behandlerDialogmeldingDTO.adresse shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.adresse
                            behandlerDialogmeldingDTO.postnummer shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.postnummer
                            behandlerDialogmeldingDTO.poststed shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.poststed
                            behandlerDialogmeldingDTO.telefon shouldBeEqualTo fastlegeResponse.fastlegekontor.telefon
                            behandlerDialogmeldingDTO.orgnummer shouldBeEqualTo fastlegeResponse.fastlegekontor.orgnummer
                            behandlerDialogmeldingDTO.kontor shouldBeEqualTo fastlegeResponse.fastlegekontor.navn
                            behandlerDialogmeldingDTO.type shouldBeEqualTo BehandlerType.FASTLEGE.name
                            behandlerDialogmeldingDTO.behandlerRef shouldBeEqualTo behandlerDialogmeldingForPersonList.first().behandlerRef.toString()
                            behandlerDialogmeldingDTO.fnr shouldBeEqualTo fastlegeResponse.fnr

                            database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_FNR,
                            ).size shouldBeEqualTo 1
                        }
                    }
                    it("should return empty list of $dtoClassName for person missing Fastlege") {
                        val validToken = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value,
                        )

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<PersonBehandlerDTO>>(response.content!!)

                            behandlerDialogmeldingList.size shouldBeEqualTo 0

                            database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR,
                            ).size shouldBeEqualTo 0
                        }
                    }

                    it("should return empty list of $dtoClassName for person with Fastlege missing foreldreEnhetHerId") {
                        val validToken = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET.value,
                        )

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<PersonBehandlerDTO>>(response.content!!)

                            behandlerDialogmeldingList.size shouldBeEqualTo 0

                            database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET,
                            ).size shouldBeEqualTo 0
                        }
                    }

                    it("should return empty list of $dtoClassName for person with Fastlege missing partnerinfo") {
                        val validToken = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO.value,
                        )

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<PersonBehandlerDTO>>(response.content!!)

                            behandlerDialogmeldingList.size shouldBeEqualTo 0

                            database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO,
                            ).size shouldBeEqualTo 0
                        }
                    }
                    it("should return empty list of $dtoClassName for person with fastlege missing fnr, hprId and herId") {
                        val validToken = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID.value,
                        )

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<PersonBehandlerDTO>>(response.content!!)

                            behandlerDialogmeldingList.size shouldBeEqualTo 0

                            database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID,
                            ).size shouldBeEqualTo 0
                        }
                    }
                }
                describe("Unhappy paths") {
                    it("should return status Unauthorized if no token is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {}
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
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
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenNoPid))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }

                    it("should return status BadRequest if invalid PersonIdent in PID is supplied") {
                        val tokenInvalidPid = generateJWTIdporten(
                            audience = externalMockEnvironment.environment.idportenTokenXClientId,
                            clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                            issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                            pid = UserConstants.ARBEIDSTAKER_FNR.value.drop(1),
                        )

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenInvalidPid))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
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
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenValidPidNoClientId))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
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
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(tokenValidPidUnauthorizedClientId))
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
