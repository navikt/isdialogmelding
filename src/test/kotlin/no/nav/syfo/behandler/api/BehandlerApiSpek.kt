package no.nav.syfo.behandler.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.behandler.database.getBehandlerDialogmeldingForArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerType
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

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
            describe("Get list of BehandlerDialogmelding for PersonIdent") {
                val url = "$behandlerPath$behandlerPersonident"
                val validToken = generateJWT(
                    externalMockEnvironment.environment.aadAppClient,
                    externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                    UserConstants.VEILEDER_IDENT,
                )
                describe("Happy path") {
                    it("should return list of BehandlerDialogmelding and store behandler if request is successful") {
                        val fastlegeResponse = generateFastlegeResponse()
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<BehandlerDialogmeldingDTO>>(response.content!!)
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
                    it("should return empty list of BehandlerDialogmelding for arbeidstaker uten fastlege") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<BehandlerDialogmeldingDTO>>(response.content!!)

                            behandlerDialogmeldingList.size shouldBeEqualTo 0

                            database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR,
                            ).size shouldBeEqualTo 0
                        }
                    }

                    it("should return empty list of BehandlerDialogmelding for arbeidstaker med fastlege uten foreldreEnhetHerId") {
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

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<BehandlerDialogmeldingDTO>>(response.content!!)

                            behandlerDialogmeldingList.size shouldBeEqualTo 0

                            database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET,
                            ).size shouldBeEqualTo 0
                        }
                    }

                    it("should return empty list of BehandlerDialogmelding for arbeidstaker med fastlege uten partnerinfo") {
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

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<BehandlerDialogmeldingDTO>>(response.content!!)

                            behandlerDialogmeldingList.size shouldBeEqualTo 0

                            database.getBehandlerDialogmeldingForArbeidstaker(
                                UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO,
                            ).size shouldBeEqualTo 0
                        }
                    }
                    it("should return empty list of BehandlerDialogmelding for arbeidstaker med fastlege som mangler fnr, hprId og herId") {
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

                            val behandlerDialogmeldingList =
                                objectMapper.readValue<List<BehandlerDialogmeldingDTO>>(response.content!!)

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

                    it("should return status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                        }
                    }

                    it("should return status BadRequest if $NAV_PERSONIDENT_HEADER with invalid PersonIdent is supplied") {
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value.drop(1))
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
        }
    }
})
