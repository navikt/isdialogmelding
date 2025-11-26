package no.nav.syfo.behandler.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.behandler.database.getBehandlerByArbeidstaker
import no.nav.syfo.behandler.database.getBehandlerKontorById
import no.nav.syfo.behandler.database.invalidateBehandler
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import no.nav.syfo.testhelper.UserConstants.OTHER_PARTNERID
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class BehandlerApiTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    private val validToken = generateJWT(
        externalMockEnvironment.environment.aadAppClient,
        externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        UserConstants.VEILEDER_IDENT,
    )

    @Nested
    @DisplayName("Get list of Behandler for Personident")
    inner class GetListOfBehandlerForPersonident {
        private val url = "$behandlerPath$behandlerPersonident"
        private val searchUrl = "$behandlerPath$search"

        @Nested
        @DisplayName("Happy path")
        inner class HappyPath {
            @Test
            fun `should return list of Behandler and store behandler if request is successful`() {
                val fastlegeResponse = generateFastlegeResponse()
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)

                    val behandlerForPersonList = database.getBehandlerByArbeidstaker(
                        ARBEIDSTAKER_FNR,
                    )
                    assertEquals(1, behandlerForPersonList.size)

                    val behandlerDTO = behandlerList.first()
                    assertEquals(fastlegeResponse.fornavn, behandlerDTO.fornavn)
                    assertEquals(fastlegeResponse.mellomnavn, behandlerDTO.mellomnavn)
                    assertEquals(fastlegeResponse.etternavn, behandlerDTO.etternavn)
                    assertEquals(fastlegeResponse.fastlegekontor.postadresse?.adresse, behandlerDTO.adresse)
                    assertEquals(fastlegeResponse.fastlegekontor.postadresse?.postnummer, behandlerDTO.postnummer)
                    assertEquals(fastlegeResponse.fastlegekontor.postadresse?.poststed, behandlerDTO.poststed)
                    assertEquals(fastlegeResponse.fastlegekontor.telefon, behandlerDTO.telefon)
                    assertEquals(fastlegeResponse.fastlegekontor.orgnummer, behandlerDTO.orgnummer)
                    assertEquals(fastlegeResponse.fastlegekontor.navn, behandlerDTO.kontor)
                    assertEquals(BehandlerArbeidstakerRelasjonstype.FASTLEGE.name, behandlerDTO.type)
                    assertEquals(behandlerForPersonList.first().behandlerRef.toString(), behandlerDTO.behandlerRef)
                    assertEquals(fastlegeResponse.fnr, behandlerDTO.fnr)
                    assertEquals(BehandlerKategori.LEGE.name, behandlerDTO.kategori)
                    assertEquals(fastlegeResponse.helsepersonellregisterId, behandlerDTO.hprId)
                }
            }

            @Test
            fun `should return list of Behandler and store behandler connected to kontor with latest dialogmeldingEnabled`() {
                database.createKontor(
                    partnerId = OTHER_PARTNERID,
                    navn = OTHER_PARTNERID.toString()
                )
                database.createKontor(
                    partnerId = PARTNERID,
                    navn = PARTNERID.toString()
                )
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)
                    val behandlerDTO = behandlerList.first()
                    assertEquals(PARTNERID.toString(), behandlerDTO.kontor)
                }
            }

            @Test
            fun `should return list of Behandler and store behandler connected to kontor with dialogmeldingEnabled`() {
                database.createKontor(
                    partnerId = OTHER_PARTNERID,
                    navn = OTHER_PARTNERID.toString()
                )
                database.createKontor(
                    partnerId = PARTNERID,
                    navn = PARTNERID.toString(),
                    dialogmeldingEnabled = false,
                )
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)
                    val behandlerDTO = behandlerList.first()
                    assertEquals(OTHER_PARTNERID.toString(), behandlerDTO.kontor)
                }
            }

            @Test
            fun `should return list of Behandler and store behandler connected to kontor with largest partnerId if dialogmelding not enabled`() {
                database.createKontor(
                    partnerId = OTHER_PARTNERID,
                    navn = OTHER_PARTNERID.toString(),
                    dialogmeldingEnabled = false,
                )
                database.createKontor(
                    partnerId = PARTNERID,
                    navn = PARTNERID.toString(),
                    dialogmeldingEnabled = false,
                )
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)
                    val behandlerDTO = behandlerList.first()
                    assertEquals(OTHER_PARTNERID.toString(), behandlerDTO.kontor)
                }
            }

            @Test
            fun `should exclude suspendert Behandler`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                        val behandlerList = body<List<BehandlerDTO>>()
                        assertEquals(1, behandlerList.size)
                        val behandlerForPersonList = database.getBehandlerByArbeidstaker(
                            ARBEIDSTAKER_FNR,
                        )
                        assertEquals(1, behandlerForPersonList.size)
                        database.setSuspendert(behandlerList[0].behandlerRef)
                    }

                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)

                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(0, behandlerList.size)
                    val behandlerForPersonList = database.getBehandlerByArbeidstaker(
                        ARBEIDSTAKER_FNR,
                    )
                    assertEquals(0, behandlerForPersonList.size)
                }
            }

            @Test
            fun `search should return list of Behandler`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }

                    val response = client.get(searchUrl) {
                        bearerAuth(validToken)
                        header("searchstring", "Scully")
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)
                }
            }

            @Test
            fun `search using post-endpoint should return list of Behandler`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }

                    val response = client.post(searchUrl) {
                        bearerAuth(validToken)
                        contentType(ContentType.Application.Json)
                        setBody(SearchRequest(searchstring = "Scully"))
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)
                }
            }

            @Test
            fun `search should exclude invalidated Behandler`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                        val behandlerList = body<List<BehandlerDTO>>()
                        assertEquals(1, behandlerList.size)
                        database.invalidateBehandler(UUID.fromString(behandlerList[0].behandlerRef))
                    }

                    val response = client.get(searchUrl) {
                        bearerAuth(validToken)
                        header("searchstring", "Scully")
                    }
                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(0, behandlerList.size)
                }
            }

            @Test
            fun `search should exclude suspendert Behandler`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                        val behandlerList = body<List<BehandlerDTO>>()
                        assertEquals(1, behandlerList.size)
                        database.setSuspendert(behandlerList[0].behandlerRef)
                    }

                    val response = client.get(searchUrl) {
                        bearerAuth(validToken)
                        header("searchstring", "Scully")
                    }
                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(0, behandlerList.size)
                }
            }

            @Test
            fun `search should remove special characters`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }

                    val response = client.get(searchUrl) {
                        bearerAuth(validToken)
                        header("searchstring", "Scu:lly, Dana: Fas,tle.gen kont:or")
                    }

                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)
                }
            }

            @Test
            fun `search with multiple strings should return list of Behandler`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }

                    val response = client.get(searchUrl) {
                        bearerAuth(validToken)
                        header("searchstring", "Dan Scully Fastlegens")
                    }

                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)
                }
            }

            @Test
            fun `search with lower case kontor navn should return list of Behandler`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }

                    val response = client.get(searchUrl) {
                        bearerAuth(validToken)
                        header("searchstring", "fastlegen")
                    }

                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(1, behandlerList.size)
                }
            }

            @Test
            fun `search with too short strings should return empty list of Behandler`() {
                testApplication {
                    val client = setupApiAndClient()
                    client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                    }.apply {
                        assertEquals(HttpStatusCode.OK, status)
                    }

                    val response = client.get(searchUrl) {
                        bearerAuth(validToken)
                        header("searchstring", "Da Sc")
                    }

                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(0, behandlerList.size)
                }
            }

            @Test
            fun `should return empty list of Behandler for arbeidstaker uten fastlege`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()

                    assertEquals(0, behandlerList.size)

                    assertEquals(
                        0,
                        database.getBehandlerByArbeidstaker(
                            UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR,
                        ).size
                    )
                }
            }

            @Test
            fun `should return empty list of Behandler for arbeidstaker med fastlege uten foreldreEnhetHerId`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()

                    assertEquals(0, behandlerList.size)

                    assertEquals(
                        0,
                        database.getBehandlerByArbeidstaker(
                            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET,
                        ).size
                    )
                }
            }

            @Test
            fun `should return empty list of Behandler for arbeidstaker med fastlege uten partnerinfo`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()

                    assertEquals(0, behandlerList.size)
                    assertEquals(
                        0,
                        database.getBehandlerByArbeidstaker(
                            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO,
                        ).size
                    )
                }
            }

            @Test
            fun `should return list of Behandler (with Kontor with largest partnerId) for arbeidstaker with fastlege with multiple partnerinfo`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()

                    assertEquals(1, behandlerList.size)

                    val behandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
                        ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO,
                    )
                    assertEquals(1, behandlerForArbeidstakerList.size)
                    val behandlerKontor =
                        database.getBehandlerKontorById(behandlerForArbeidstakerList.first().kontorId)
                    assertEquals(OTHER_PARTNERID.toString(), behandlerKontor.partnerId)
                }
            }

            @Test
            fun `should return empty list of Behandler for arbeidstaker med fastlege som mangler fnr, hprId og herId`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID.value)
                    }

                    assertEquals(HttpStatusCode.OK, response.status)
                    val behandlerList = response.body<List<BehandlerDTO>>()
                    assertEquals(0, behandlerList.size)

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
            fun `should return status BadRequest if no NAV_PERSONIDENT_HEADER is supplied`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                    }

                    assertEquals(HttpStatusCode.BadRequest, response.status)
                }
            }

            @Test
            fun `should return status BadRequest if NAV_PERSONIDENT_HEADER with invalid Personident is supplied`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value.drop(1))
                    }

                    assertEquals(HttpStatusCode.BadRequest, response.status)
                }
            }

            @Test
            fun `should return status Forbidden if denied access to personident supplied in NAV_PERSONIDENT_HEADER`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_VEILEDER_NO_ACCESS.value)
                    }

                    assertEquals(HttpStatusCode.Forbidden, response.status)
                }
            }
        }
    }

    @Nested
    @DisplayName("Get behandler for behandlerRef")
    inner class GetBehandlerForBehandlerRef {
        private val behandlerRef = UUID.randomUUID()
        private val url = "$behandlerPath/$behandlerRef"
        private val behandler = generateBehandler(
            behandlerRef = behandlerRef,
            partnerId = PARTNERID,
        )

        @Nested
        @DisplayName("Happy path")
        inner class HappyPath {
            @Test
            fun `should return behandler for behandlerRef`() {
                database.createBehandlerForArbeidstaker(
                    behandler = behandler,
                    arbeidstakerPersonident = ARBEIDSTAKER_FNR,
                )
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                    }

                    val behandlerDTO = response.body<BehandlerDTO>()
                    assertEquals(behandlerRef.toString(), behandlerDTO.behandlerRef)
                    assertEquals("Dana", behandlerDTO.fornavn)
                }
            }
        }

        @Nested
        @DisplayName("Unhappy path")
        inner class UnhappyPath {
            @Test
            fun `should return status NotFound for non-matching behandlerRef`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                    }

                    assertEquals(HttpStatusCode.NotFound, response.status)
                }
            }

            @Test
            fun `should return status Unauthorized if no token is supplied`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url)

                    assertEquals(HttpStatusCode.Unauthorized, response.status)
                }
            }

            @Test
            fun `should return status BadRequest if invalid behandlerRef`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get("$behandlerPath/123abc") {
                        bearerAuth(validToken)
                    }
                    assertEquals(HttpStatusCode.BadRequest, response.status)
                }
            }
        }
    }
}
