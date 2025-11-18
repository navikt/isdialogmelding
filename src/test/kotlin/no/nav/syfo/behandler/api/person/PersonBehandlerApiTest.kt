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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PersonBehandlerApiTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val url = "$personApiBehandlerPath$personBehandlerSelfPath"

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {
        @Test
        fun `should return list of PersonBehandlerDTO and store behandler if request is successful`() {
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

                assertEquals(HttpStatusCode.OK, response.status)

                val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                assertEquals(1, personBehandlerList.size)

                val behandlerForPersonList = database.getBehandlerByArbeidstaker(
                    UserConstants.ARBEIDSTAKER_FNR,
                )
                assertEquals(1, behandlerForPersonList.size)

                val personBehandlerDTO = personBehandlerList.first()
                assertEquals(fastlegeResponse.fornavn, personBehandlerDTO.fornavn)
                assertEquals(fastlegeResponse.mellomnavn, personBehandlerDTO.mellomnavn)
                assertEquals(fastlegeResponse.etternavn, personBehandlerDTO.etternavn)
                assertEquals(fastlegeResponse.fastlegekontor.postadresse?.adresse, personBehandlerDTO.adresse)
                assertEquals(fastlegeResponse.fastlegekontor.postadresse?.postnummer, personBehandlerDTO.postnummer)
                assertEquals(fastlegeResponse.fastlegekontor.postadresse?.poststed, personBehandlerDTO.poststed)
                assertEquals(fastlegeResponse.fastlegekontor.telefon, personBehandlerDTO.telefon)
                assertEquals(fastlegeResponse.fastlegekontor.orgnummer, personBehandlerDTO.orgnummer)
                assertEquals(fastlegeResponse.fastlegekontor.navn, personBehandlerDTO.kontor)
                assertEquals(BehandlerArbeidstakerRelasjonstype.FASTLEGE.name, personBehandlerDTO.type)
                assertEquals(BehandlerKategori.LEGE.name, personBehandlerDTO.kategori)
                assertEquals(behandlerForPersonList.first().behandlerRef.toString(), personBehandlerDTO.behandlerRef)
                assertEquals(fastlegeResponse.fnr, personBehandlerDTO.fnr)

                assertEquals(
                    1,
                    database.getBehandlerByArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size
                )
            }
        }

        @Test
        fun `should return empty list of PersonBehandlerDTO for person missing Fastlege`() {
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

                assertEquals(HttpStatusCode.OK, response.status)

                val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                assertEquals(0, personBehandlerList.size)
                assertEquals(
                    0,
                    database.getBehandlerByArbeidstaker(
                        UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR,
                    ).size
                )
            }
        }

        @Test
        fun `should return empty list of PersonBehandlerDTO for person with Fastlege missing foreldreEnhetHerId`() {
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

                assertEquals(HttpStatusCode.OK, response.status)

                val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                assertEquals(0, personBehandlerList.size)
                assertEquals(
                    0,
                    database.getBehandlerByArbeidstaker(
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET,
                    ).size
                )
            }
        }

        @Test
        fun `should return empty list of PersonBehandlerDTO for person with Fastlege missing partnerinfo`() {
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

                assertEquals(HttpStatusCode.OK, response.status)

                val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                assertEquals(0, personBehandlerList.size)

                assertEquals(
                    0,
                    database.getBehandlerByArbeidstaker(
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO,
                    ).size
                )
            }
        }

        @Test
        fun `should return empty list of PersonBehandlerDTO for person with fastlege missing fnr, hprId and herId`() {
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

                assertEquals(HttpStatusCode.OK, response.status)

                val personBehandlerList = response.body<List<PersonBehandlerDTO>>()
                assertEquals(0, personBehandlerList.size)

                assertEquals(
                    0,
                    database.getBehandlerByArbeidstaker(
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID,
                    ).size
                )
            }
        }
    }

    @Nested
    @DisplayName("Unhappy paths")
    inner class UnhappyPaths {
        @Test
        fun `should return status Unauthorized if no token is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get(url)

                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `should return status BadRequest if no PID is supplied`() {
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

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        @Test
        fun `should return status BadRequest if invalid Personident in PID is supplied`() {
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

                assertEquals(HttpStatusCode.BadRequest, response.status)
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
                val response = client.get(url) {
                    bearerAuth(tokenValidPidNoClientId)
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
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
                val response = client.get(url) {
                    bearerAuth(tokenValidPidUnauthorizedClientId)
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
            }
        }
    }
}
