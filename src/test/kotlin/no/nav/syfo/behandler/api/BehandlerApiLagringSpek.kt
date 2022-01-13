package no.nav.syfo.behandler.api

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.behandler.database.getBehandlerDialogmeldingForArbeidstaker
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class BehandlerApiLagringSpek : Spek({
    describe(BehandlerApiLagringSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            application.testApiModule(externalMockEnvironment = externalMockEnvironment)

            afterEachTest {
                database.dropData()
            }

            val url = "$behandlerPath$behandlerPersonident"
            val validToken = generateJWT(
                externalMockEnvironment.environment.aadAppClient,
                externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                UserConstants.VEILEDER_IDENT,
            )

            describe("Get behandler for arbeidstaker creates behandler-arbeidstaker relation if missing") {
                it("creates relation for arbeidstaker with fastlege") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 1
                    behandlerForArbeidstakerList.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("creates relation only once for multiple gets for arbeidstaker with fastlege") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    var behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 1
                    behandlerForArbeidstakerList.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }
                    behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 1
                    behandlerForArbeidstakerList.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("creates two relations with equal behandler for gets for arbeidstakere with equal fastlege") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ANNEN_ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 1
                    val behandlerForAnnenArbeidstakerList =
                        database.getBehandlerDialogmeldingForArbeidstaker(
                            UserConstants.ANNEN_ARBEIDSTAKER_FNR,
                        )
                    behandlerForAnnenArbeidstakerList.size shouldBeEqualTo 1
                    behandlerForArbeidstakerList.first() shouldBeEqualTo behandlerForAnnenArbeidstakerList.first()
                }
                it("creates relation for arbeidstaker with fastlege missing fnr") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 1
                    behandlerForArbeidstakerList.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("creates relation for arbeidstaker with fastlege missing hprId") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_HPRID.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_HPRID,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 1
                    behandlerForArbeidstakerList.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("creates relation for arbeidstaker with fastlege missing herId") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_HERID.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_HERID,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 1
                    behandlerForArbeidstakerList.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("creates no relation for arbeidstaker with fastlege missing fnr, hprId and herId") {
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
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 0
                }
                it("creates no relation when arbeidstakers fastlege equal to behandler in latest relation for arbeidstaker") {
                    database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = generateFastlegeResponse(
                            UserConstants.FASTLEGE_FNR,
                            UserConstants.HERID
                        ).toBehandler(UserConstants.PARTNERID),
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("creates relation when arbeidstakers fastlege equal to behandler in relation for other arbeidstaker") {
                    database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = generateFastlegeResponse(
                            UserConstants.FASTLEGE_FNR,
                            UserConstants.HERID
                        ).toBehandler(UserConstants.PARTNERID),
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_MED_FASTLEGE_MED_ANNEN_HERID
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("creates relation when arbeidstakers fastlege not equal to behandler in latest relation for arbeidstaker") {
                    val behandlerRef = database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = generateFastlegeResponse(
                            UserConstants.FASTLEGE_FNR,
                            UserConstants.OTHER_HERID
                        ).toBehandler(UserConstants.OTHER_PARTNERID),
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 2
                    behandlerForArbeidstakerList[0].behandlerRef shouldNotBeEqualTo behandlerRef
                    behandlerForArbeidstakerList[1].behandlerRef shouldBeEqualTo behandlerRef
                }

                it("creates relation when relation exists for arbeidstakers fastlege but other behandler is in latest relation for arbeidstaker") {
                    val behandlerRef = database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = generateFastlegeResponse(
                            UserConstants.FASTLEGE_FNR,
                            UserConstants.HERID
                        ).toBehandler(UserConstants.PARTNERID),
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )
                    val otherBehandlerRef = database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = generateFastlegeResponse(
                            UserConstants.FASTLEGE_ANNEN_FNR,
                            UserConstants.OTHER_HERID
                        ).toBehandler(UserConstants.OTHER_PARTNERID),
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 3
                    behandlerForArbeidstakerList[0].behandlerRef shouldBeEqualTo behandlerRef
                    behandlerForArbeidstakerList[1].behandlerRef shouldBeEqualTo otherBehandlerRef
                    behandlerForArbeidstakerList[2].behandlerRef shouldBeEqualTo behandlerRef
                }

                it("creates relation when arbeidstakers fastlege has partnerId equal to behandler in relation for other arbeidstaker") {
                    val behandlerRef = database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = generateFastlegeResponse(
                            UserConstants.FASTLEGE_FNR,
                            UserConstants.HERID
                        ).toBehandler(UserConstants.PARTNERID),
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(
                                NAV_PERSONIDENT_HEADER,
                                UserConstants.ARBEIDSTAKER_MED_ANNEN_FASTLEGE_SAMME_PARTNERINFO.value
                            )
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_MED_ANNEN_FASTLEGE_SAMME_PARTNERINFO,
                    )
                    behandlerForArbeidstakerList.size shouldBeEqualTo 1
                    behandlerForArbeidstakerList[0].behandlerRef shouldNotBeEqualTo behandlerRef
                }
            }
        }
    }
})
