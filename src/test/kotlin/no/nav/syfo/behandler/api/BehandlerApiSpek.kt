package no.nav.syfo.behandler.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import no.nav.syfo.testhelper.UserConstants.OTHER_PARTNERID
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID

class BehandlerApiSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        application.testApiModule(externalMockEnvironment = externalMockEnvironment)

        afterEachTest {
            database.dropData()
        }

        describe(BehandlerApiSpek::class.java.simpleName) {
            val validToken = generateJWT(
                externalMockEnvironment.environment.aadAppClient,
                externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                UserConstants.VEILEDER_IDENT,
            )
            describe("Get list of Behandler for Personident") {
                val url = "$behandlerPath$behandlerPersonident"
                val searchUrl = "$behandlerPath$search"
                describe("Happy path") {
                    it("should return list of Behandler and store behandler if request is successful") {
                        val fastlegeResponse = generateFastlegeResponse()
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 1

                            val behandlerForPersonList = database.getBehandlerByArbeidstaker(
                                ARBEIDSTAKER_FNR,
                            )
                            behandlerForPersonList.size shouldBeEqualTo 1

                            val behandlerDTO = behandlerList.first()
                            behandlerDTO.fornavn shouldBeEqualTo fastlegeResponse.fornavn
                            behandlerDTO.mellomnavn shouldBeEqualTo fastlegeResponse.mellomnavn
                            behandlerDTO.etternavn shouldBeEqualTo fastlegeResponse.etternavn
                            behandlerDTO.adresse shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.adresse
                            behandlerDTO.postnummer shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.postnummer
                            behandlerDTO.poststed shouldBeEqualTo fastlegeResponse.fastlegekontor.postadresse?.poststed
                            behandlerDTO.telefon shouldBeEqualTo fastlegeResponse.fastlegekontor.telefon
                            behandlerDTO.orgnummer shouldBeEqualTo fastlegeResponse.fastlegekontor.orgnummer
                            behandlerDTO.kontor shouldBeEqualTo fastlegeResponse.fastlegekontor.navn
                            behandlerDTO.type shouldBeEqualTo BehandlerArbeidstakerRelasjonstype.FASTLEGE.name
                            behandlerDTO.behandlerRef shouldBeEqualTo behandlerForPersonList.first().behandlerRef.toString()
                            behandlerDTO.fnr shouldBeEqualTo fastlegeResponse.fnr
                        }
                    }
                    it("search should return list of Behandler") {
                        generateFastlegeResponse()
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                        }
                        with(
                            handleRequest(HttpMethod.Get, searchUrl) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader("searchstring", "Scully")
                            }
                        ) {
                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 1
                        }
                    }
                    it("search should exclude invalidated Behandler") {
                        generateFastlegeResponse()
                        val behandlerRef = with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val behandlerList = objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 1
                            UUID.fromString(behandlerList[0].behandlerRef)
                        }
                        database.invalidateBehandler(behandlerRef)
                        with(
                            handleRequest(HttpMethod.Get, searchUrl) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader("searchstring", "Scully")
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val behandlerList = objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 0
                        }
                    }
                    it("search should remove special characters") {
                        generateFastlegeResponse()
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                        }
                        with(
                            handleRequest(HttpMethod.Get, searchUrl) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader("searchstring", "Scu:lly, Dana: Fas,tle.gen kont:or")
                            }
                        ) {
                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 1
                        }
                    }
                    it("search with multiple strings should return list of Behandler") {
                        generateFastlegeResponse()
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                        }
                        with(
                            handleRequest(HttpMethod.Get, searchUrl) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader("searchstring", "Dan Scully Fastlegens")
                            }
                        ) {
                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 1
                        }
                    }
                    it("search with lower case kontor navn should return list of Behandler") {
                        generateFastlegeResponse()
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                        }
                        with(
                            handleRequest(HttpMethod.Get, searchUrl) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader("searchstring", "fastlegen")
                            }
                        ) {
                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 1
                        }
                    }
                    it("search with too short strings should return empty list of Behandler") {
                        generateFastlegeResponse()
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                        }
                        with(
                            handleRequest(HttpMethod.Get, searchUrl) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader("searchstring", "Da Sc")
                            }
                        ) {
                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 0
                        }
                    }
                    it("should return empty list of Behandler for arbeidstaker uten fastlege") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)

                            behandlerList.size shouldBeEqualTo 0

                            database.getBehandlerByArbeidstaker(
                                UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR,
                            ).size shouldBeEqualTo 0
                        }
                    }

                    it("should return empty list of Behandler for arbeidstaker med fastlege uten foreldreEnhetHerId") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(
                                    NAV_PERSONIDENT_HEADER,
                                    UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET.value
                                )
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)

                            behandlerList.size shouldBeEqualTo 0

                            database.getBehandlerByArbeidstaker(
                                UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET,
                            ).size shouldBeEqualTo 0
                        }
                    }

                    it("should return empty list of Behandler for arbeidstaker med fastlege uten partnerinfo") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(
                                    NAV_PERSONIDENT_HEADER,
                                    UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO.value
                                )
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)

                            behandlerList.size shouldBeEqualTo 0

                            database.getBehandlerByArbeidstaker(
                                UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO,
                            ).size shouldBeEqualTo 0
                        }
                    }

                    it("should return list of Behandler (with Kontor with largest partnerId) for arbeidstaker with fastlege with multiple partnerinfo") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(
                                    NAV_PERSONIDENT_HEADER,
                                    UserConstants.ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO.value
                                )
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)

                            behandlerList.size shouldBeEqualTo 1

                            val behandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
                                UserConstants.ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO,
                            )
                            behandlerForArbeidstakerList.size shouldBeEqualTo 1
                            val behandlerKontor =
                                database.getBehandlerKontorById(behandlerForArbeidstakerList.first().kontorId)
                            behandlerKontor.partnerId shouldBeEqualTo OTHER_PARTNERID.toString()
                        }
                    }

                    it("should return empty list of Behandler for arbeidstaker med fastlege som mangler fnr, hprId og herId") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(
                                    NAV_PERSONIDENT_HEADER,
                                    UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID.value
                                )
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)

                            behandlerList.size shouldBeEqualTo 0

                            database.getBehandlerByArbeidstaker(
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

                    it("should return status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }

                    it("should return status BadRequest if $NAV_PERSONIDENT_HEADER with invalid Personident is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value.drop(1))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }
                    it("should return status Forbidden if denied access to personident supplied in $NAV_PERSONIDENT_HEADER") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_VEILEDER_NO_ACCESS.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            describe("Get behandler for behandlerRef") {
                val behandlerRef = UUID.randomUUID()
                val url = "$behandlerPath/$behandlerRef"
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
                }
            }
        }
    }
})
