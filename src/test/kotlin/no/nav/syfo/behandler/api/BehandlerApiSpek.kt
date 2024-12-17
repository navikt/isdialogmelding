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
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class BehandlerApiSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

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
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()
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
                        behandlerDTO.kategori shouldBeEqualTo BehandlerKategori.LEGE.name
                        behandlerDTO.hprId shouldBeEqualTo fastlegeResponse.helsepersonellregisterId
                    }
                }
                it("should return list of Behandler and store behandler connected to kontor with latest dialogmeldingEnabled") {
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

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 1
                        val behandlerDTO = behandlerList.first()
                        behandlerDTO.kontor shouldBeEqualTo PARTNERID.toString()
                    }
                }
                it("should return list of Behandler and store behandler connected to kontor with dialogmeldingEnabled") {
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

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 1
                        val behandlerDTO = behandlerList.first()
                        behandlerDTO.kontor shouldBeEqualTo OTHER_PARTNERID.toString()
                    }
                }
                it("should return list of Behandler and store behandler connected to kontor with largest partnerId if dialogmelding not enabled") {
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

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 1
                        val behandlerDTO = behandlerList.first()
                        behandlerDTO.kontor shouldBeEqualTo OTHER_PARTNERID.toString()
                    }
                }
                it("should exclude suspendert Behandler") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.OK
                            val behandlerList = body<List<BehandlerDTO>>()
                            behandlerList.size shouldBeEqualTo 1
                            val behandlerForPersonList = database.getBehandlerByArbeidstaker(
                                ARBEIDSTAKER_FNR,
                            )
                            behandlerForPersonList.size shouldBeEqualTo 1
                            database.setSuspendert(behandlerList[0].behandlerRef)
                        }

                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 0
                        val behandlerForPersonList = database.getBehandlerByArbeidstaker(
                            ARBEIDSTAKER_FNR,
                        )
                        behandlerForPersonList.size shouldBeEqualTo 0
                    }
                }
                it("search should return list of Behandler") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.OK
                        }

                        val response = client.get(searchUrl) {
                            bearerAuth(validToken)
                            header("searchstring", "Scully")
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 1
                    }
                }
                it("search should exclude invalidated Behandler") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.OK
                            val behandlerList = body<List<BehandlerDTO>>()
                            behandlerList.size shouldBeEqualTo 1
                            database.invalidateBehandler(UUID.fromString(behandlerList[0].behandlerRef))
                        }

                        val response = client.get(searchUrl) {
                            bearerAuth(validToken)
                            header("searchstring", "Scully")
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 0
                    }
                }
                it("search should exclude suspendert Behandler") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.OK
                            val behandlerList = body<List<BehandlerDTO>>()
                            behandlerList.size shouldBeEqualTo 1
                            database.setSuspendert(behandlerList[0].behandlerRef)
                        }

                        val response = client.get(searchUrl) {
                            bearerAuth(validToken)
                            header("searchstring", "Scully")
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 0
                    }
                }
                it("search should remove special characters") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.OK
                        }

                        val response = client.get(searchUrl) {
                            bearerAuth(validToken)
                            header("searchstring", "Scu:lly, Dana: Fas,tle.gen kont:or")
                        }

                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 1
                    }
                }
                it("search with multiple strings should return list of Behandler") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.OK
                        }

                        val response = client.get(searchUrl) {
                            bearerAuth(validToken)
                            header("searchstring", "Dan Scully Fastlegens")
                        }

                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 1
                    }
                }
                it("search with lower case kontor navn should return list of Behandler") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.OK
                        }

                        val response = client.get(searchUrl) {
                            bearerAuth(validToken)
                            header("searchstring", "fastlegen")
                        }

                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 1
                    }
                }
                it("search with too short strings should return empty list of Behandler") {
                    testApplication {
                        val client = setupApiAndClient()
                        client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value)
                        }.apply {
                            status shouldBeEqualTo HttpStatusCode.OK
                        }

                        val response = client.get(searchUrl) {
                            bearerAuth(validToken)
                            header("searchstring", "Da Sc")
                        }

                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 0
                    }
                }
                it("should return empty list of Behandler for arbeidstaker uten fastlege") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()

                        behandlerList.size shouldBeEqualTo 0

                        database.getBehandlerByArbeidstaker(
                            UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR,
                        ).size shouldBeEqualTo 0
                    }
                }

                it("should return empty list of Behandler for arbeidstaker med fastlege uten foreldreEnhetHerId") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()

                        behandlerList.size shouldBeEqualTo 0

                        database.getBehandlerByArbeidstaker(
                            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET,
                        ).size shouldBeEqualTo 0
                    }
                }

                it("should return empty list of Behandler for arbeidstaker med fastlege uten partnerinfo") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()

                        behandlerList.size shouldBeEqualTo 0
                        database.getBehandlerByArbeidstaker(
                            UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO,
                        ).size shouldBeEqualTo 0
                    }
                }

                it("should return list of Behandler (with Kontor with largest partnerId) for arbeidstaker with fastlege with multiple partnerinfo") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()

                        behandlerList.size shouldBeEqualTo 1

                        val behandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
                            ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO,
                        )
                        behandlerForArbeidstakerList.size shouldBeEqualTo 1
                        val behandlerKontor =
                            database.getBehandlerKontorById(behandlerForArbeidstakerList.first().kontorId)
                        behandlerKontor.partnerId shouldBeEqualTo OTHER_PARTNERID.toString()
                    }
                }

                it("should return empty list of Behandler for arbeidstaker med fastlege som mangler fnr, hprId og herId") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val behandlerList = response.body<List<BehandlerDTO>>()
                        behandlerList.size shouldBeEqualTo 0

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

                it("should return status BadRequest if no $NAV_PERSONIDENT_HEADER is supplied") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }

                it("should return status BadRequest if $NAV_PERSONIDENT_HEADER with invalid Personident is supplied") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR.value.drop(1))
                        }

                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }
                it("should return status Forbidden if denied access to personident supplied in $NAV_PERSONIDENT_HEADER") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_VEILEDER_NO_ACCESS.value)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.Forbidden
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
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        val behandlerDTO = response.body<BehandlerDTO>()
                        behandlerDTO.behandlerRef shouldBeEqualTo behandlerRef.toString()
                        behandlerDTO.fornavn shouldBeEqualTo "Dana"
                    }
                }
            }
            describe("Unhappy path") {
                it("should return status NotFound for non-matching behandlerRef") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                        }

                        response.status shouldBeEqualTo HttpStatusCode.NotFound
                    }
                }
                it("should return status Unauthorized if no token is supplied") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url)

                        response.status shouldBeEqualTo HttpStatusCode.Unauthorized
                    }
                }
                it("should return status BadRequest if invalid behandlerRef") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get("$behandlerPath/123abc") {
                            bearerAuth(validToken)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }
            }
        }
    }
})
