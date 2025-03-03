package no.nav.syfo.behandler.api.person

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.behandler.database.getBehandlerByArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class PersonBehandlerApiSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

    afterEachTest {
        database.dropData()
    }

    describe(PersonBehandlerApiSpek::class.java.simpleName) {
        describe("Get list of Behandler for Personident") {
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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                        personBehandlerList.size shouldBeEqualTo 1

                        val behandlerForPersonList = database.getBehandlerByArbeidstaker(
                            UserConstants.ARBEIDSTAKER_FNR,
                        )
                        behandlerForPersonList.size shouldBeEqualTo 1

                        val personBehandlerDTO = personBehandlerList.first()
                        personBehandlerDTO.fornavn shouldBeEqualTo fastlegeResponse.fornavn
                        personBehandlerDTO.mellomnavn shouldBeEqualTo fastlegeResponse.mellomnavn
                        personBehandlerDTO.etternavn shouldBeEqualTo fastlegeResponse.etternavn
                        personBehandlerDTO.adresse shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.adresse
                        personBehandlerDTO.postnummer shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.postnummer
                        personBehandlerDTO.poststed shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.poststed
                        personBehandlerDTO.telefon shouldBeEqualTo fastlegeResponse.fastlegekontor.telefon
                        personBehandlerDTO.orgnummer shouldBeEqualTo fastlegeResponse.fastlegekontor.orgnummer
                        personBehandlerDTO.kontor shouldBeEqualTo fastlegeResponse.fastlegekontor.navn
                        personBehandlerDTO.type shouldBeEqualTo BehandlerArbeidstakerRelasjonstype.FASTLEGE.name
                        personBehandlerDTO.kategori shouldBeEqualTo BehandlerKategori.LEGE.name
                        personBehandlerDTO.behandlerRef shouldBeEqualTo behandlerForPersonList.first().behandlerRef.toString()
                        personBehandlerDTO.fnr shouldBeEqualTo fastlegeResponse.fnr

                        database.getBehandlerByArbeidstaker(
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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                        personBehandlerList.size shouldBeEqualTo 0
                        database.getBehandlerByArbeidstaker(
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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                        personBehandlerList.size shouldBeEqualTo 0
                        database.getBehandlerByArbeidstaker(
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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                        personBehandlerList.size shouldBeEqualTo 0

                        database.getBehandlerByArbeidstaker(
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

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                        personBehandlerList.size shouldBeEqualTo 0

                        database.getBehandlerByArbeidstaker(
                            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID,
                        ).size shouldBeEqualTo 0
                    }
                }
            }
            describe("Unhappy paths") {
                it("should return status Unauthorized if no token is supplied") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url)

                        response.status shouldBeEqualTo HttpStatusCode.Unauthorized
                    }
                }

                it("should return status BadRequest if no PID is supplied") {
                    val tokenNoPid = generateJWTIdporten(
                        audience = externalMockEnvironment.environment.idportenTokenXClientId,
                        clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                        issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                        pid = null,
                    )

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(tokenNoPid)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }

                it("should return status BadRequest if invalid Personident in PID is supplied") {
                    val tokenInvalidPid = generateJWTIdporten(
                        audience = externalMockEnvironment.environment.idportenTokenXClientId,
                        clientId = externalMockEnvironment.environment.aapSoknadApiClientId,
                        issuer = externalMockEnvironment.wellKnownInternalIdportenTokenX.issuer,
                        pid = UserConstants.ARBEIDSTAKER_FNR.value.drop(1),
                    )

                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(tokenInvalidPid)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
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
                        val response = client.get(url) {
                            bearerAuth(tokenValidPidNoClientId)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
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
                        val response = client.get(url) {
                            bearerAuth(tokenValidPidUnauthorizedClientId)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Forbidden
                    }
                }
            }
        }
    }
})
