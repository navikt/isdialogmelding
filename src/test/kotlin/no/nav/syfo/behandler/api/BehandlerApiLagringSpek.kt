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

            describe("Get list of BehandlerDialogmelding with empty database") {
                it("should store behandler for arbeidstaker with fastlege") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerDialogmeldingForArbeidstaker = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstaker.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstaker.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("multiple gets should store behandler once for arbeidstaker with fastlege") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    var behandlerDialogmeldingForArbeidstaker = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstaker.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstaker.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }
                    behandlerDialogmeldingForArbeidstaker = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstaker.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstaker.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("gets for arbeidstakere with equal fastlege should store behandler once") {
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

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 1
                    val behandlerDialogmeldingForAnnenArbeidstakerList =
                        database.getBehandlerDialogmeldingForArbeidstaker(
                            UserConstants.ANNEN_ARBEIDSTAKER_FNR,
                        )
                    behandlerDialogmeldingForAnnenArbeidstakerList.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstakerList.first() shouldBeEqualTo behandlerDialogmeldingForAnnenArbeidstakerList.first()
                }
                it("should store behandler for arbeidstaker with fastlege missing fnr") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_FNR_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerDialogmeldingForArbeidstaker = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_FNR_FNR,
                    )
                    behandlerDialogmeldingForArbeidstaker.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstaker.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("should store behandler for arbeidstaker with fastlege missing hprId") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_HPRID_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerDialogmeldingForArbeidstaker = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_HPRID_FNR,
                    )
                    behandlerDialogmeldingForArbeidstaker.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstaker.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("should store behandler for arbeidstaker with fastlege missing herId") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_HERID_FNR.value)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerDialogmeldingForArbeidstaker = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_HERID_FNR,
                    )
                    behandlerDialogmeldingForArbeidstaker.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstaker.first().partnerId shouldBeEqualTo UserConstants.PARTNERID.toString()
                }
                it("should not store behandler for arbeidstaker with fastlege missing fnr, hprId and herId") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(
                                NAV_PERSONIDENT_HEADER,
                                UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_FNR_HPRID_HERID_FNR.value
                            )
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerDialogmeldingForArbeidstaker = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_FNR_HPRID_HERID_FNR,
                    )
                    behandlerDialogmeldingForArbeidstaker.size shouldBeEqualTo 0
                }
            }
            describe("Get list of BehandlerDialogmelding with behandlere in database") {
                it("should not store behandler when fastlege is latest behandler stored for arbeidstaker") {
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
                it("should store behandler for arbeidstaker when fastlege is stored for other arbeidstaker") {
                    database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = generateFastlegeResponse(
                            UserConstants.FASTLEGE_FNR,
                            UserConstants.HERID
                        ).toBehandler(UserConstants.PARTNERID),
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_ANNEN_FASTLEGE_HERID_FNR
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
                it("should store behandler when fastlege is not latest behandler for arbeidstaker") {
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

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 2
                    behandlerDialogmeldingForArbeidstakerList[0].behandlerRef shouldNotBeEqualTo behandlerRef
                    behandlerDialogmeldingForArbeidstakerList[1].behandlerRef shouldBeEqualTo behandlerRef
                }

                it("should store behandler for arbeidstaker when fastlege is stored for arbeidstaker but other fastlege is latest behandler") {
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

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 3
                    behandlerDialogmeldingForArbeidstakerList[0].behandlerRef shouldBeEqualTo behandlerRef
                    behandlerDialogmeldingForArbeidstakerList[1].behandlerRef shouldBeEqualTo otherBehandlerRef
                    behandlerDialogmeldingForArbeidstakerList[2].behandlerRef shouldBeEqualTo behandlerRef
                }

                it("should store behandler for arbeidstaker when other fastlege with equal partnerId exists for other arbeidstaker") {
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
                                UserConstants.ARBEIDSTAKER_ANNEN_FASTLEGE_SAMME_PARTNERINFO_FNR.value
                            )
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_ANNEN_FASTLEGE_SAMME_PARTNERINFO_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstakerList[0].behandlerRef shouldNotBeEqualTo behandlerRef
                }
            }
        }
    }
})
