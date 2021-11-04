package no.nav.syfo.behandler

import io.ktor.server.testing.*
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.database.getBehandlerDialogmeldingForArbeidstaker
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.FastlegeResponse
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoResponse
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.testhelper.generator.generatePartnerinfoResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class BehandlerServiceSpek : Spek({

    val anyToken = "token"
    val anyCallId = "callId"
    val partnerId = 1
    val otherPartnerId = 2
    val parentHerId = 7
    val otherParentHerId = 8

    fun FastlegeClient.mockResponse(personIdentNumber: PersonIdentNumber, response: FastlegeResponse) {
        coEvery {
            this@mockResponse.fastlege(
                personIdentNumber,
                anyToken,
                anyCallId
            )
        } returns response
    }

    fun PartnerinfoClient.mockResponse(parentHerId: Int, response: PartnerinfoResponse) {
        coEvery {
            this@mockResponse.partnerinfo(
                parentHerId.toString(),
                anyToken,
                anyCallId
            )
        } returns response
    }

    describe("BehandlerService") {
        with(TestApplicationEngine()) {
            start()

            val fastlegeClientMock = mockk<FastlegeClient>()
            val partnerinfoClientMock = mockk<PartnerinfoClient>()
            val externalMockEnvironment = ExternalMockEnvironment()
            val database = externalMockEnvironment.database
            val behandlerService = BehandlerService(
                fastlegeClient = fastlegeClientMock,
                partnerinfoClient = partnerinfoClientMock,
                database = database
            )

            beforeGroup {
                externalMockEnvironment.startExternalMocks()
            }

            beforeEachTest {
                clearAllMocks()
            }

            afterEachTest {
                database.dropData()
            }

            afterGroup {
                externalMockEnvironment.stopExternalMocks()
            }

            describe("getBehandlere uten behandlere i database") {
                it("getBehandlere lagrer behandler for arbeidstaker med fastlege") {
                    fastlegeClientMock.mockResponse(
                        UserConstants.ARBEIDSTAKER_FNR,
                        generateFastlegeResponse(parentHerId)
                    )
                    partnerinfoClientMock.mockResponse(parentHerId, generatePartnerinfoResponse(partnerId))

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("getBehandlere kallt flere ganger lagrer én behandler for arbeidstaker med fastlege") {
                    fastlegeClientMock.mockResponse(
                        UserConstants.ARBEIDSTAKER_FNR,
                        generateFastlegeResponse(parentHerId)
                    )
                    partnerinfoClientMock.mockResponse(parentHerId, generatePartnerinfoResponse(partnerId))

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("getBehandlere lagrer én behandler for to arbeidstakere med samme fastlege") {
                    val fastlegeResponse = generateFastlegeResponse(parentHerId)
                    fastlegeClientMock.mockResponse(
                        UserConstants.ARBEIDSTAKER_FNR,
                        fastlegeResponse
                    )
                    fastlegeClientMock.mockResponse(
                        UserConstants.ANNEN_ARBEIDSTAKER_FNR,
                        fastlegeResponse
                    )
                    partnerinfoClientMock.mockResponse(parentHerId, generatePartnerinfoResponse(partnerId))

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 1

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ANNEN_ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    val behandlerDialogmeldingForAnnenArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ANNEN_ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForAnnenArbeidstakerList.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstakerList.first() shouldBeEqualTo behandlerDialogmeldingForAnnenArbeidstakerList.first()
                }
            }
            describe("getBehandlere med behandlere i databasen") {
                it("getBehandlere lagrer ikke ny behandler når fastlege er siste lagret behandler for arbeidstaker") {
                    val fastlegeResponse = generateFastlegeResponse(parentHerId)
                    behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_FNR,
                        fastlegeResponse.toBehandler(partnerId),
                    )
                    fastlegeClientMock.mockResponse(UserConstants.ARBEIDSTAKER_FNR, fastlegeResponse)
                    partnerinfoClientMock.mockResponse(parentHerId, generatePartnerinfoResponse(partnerId))

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("getBehandlere lagrer ny kobling til behandler for arbeidstaker når samme behandler finnes for annen arbeidstaker") {
                    val fastlegeResponse = generateFastlegeResponse(parentHerId)
                    behandlerService.createBehandlerDialogmelding(
                        UserConstants.ANNEN_ARBEIDSTAKER_FNR,
                        fastlegeResponse.toBehandler(partnerId),
                    )
                    fastlegeClientMock.mockResponse(UserConstants.ARBEIDSTAKER_FNR, fastlegeResponse)
                    partnerinfoClientMock.mockResponse(parentHerId, generatePartnerinfoResponse(partnerId))

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("getBehandlere lagrer ny behandler for arbeidstaker når fastlege er annen enn siste lagret behandler") {
                    val behandlerRef = behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_FNR,
                        generateFastlegeResponse(otherParentHerId).toBehandler(otherPartnerId),
                    ).behandlerRef
                    fastlegeClientMock.mockResponse(
                        UserConstants.ARBEIDSTAKER_FNR,
                        generateFastlegeResponse(parentHerId),
                    )
                    partnerinfoClientMock.mockResponse(parentHerId, generatePartnerinfoResponse(partnerId))

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 2
                    behandlerDialogmeldingForArbeidstakerList[0].behandlerRef shouldNotBeEqualTo behandlerRef
                    behandlerDialogmeldingForArbeidstakerList[1].behandlerRef shouldBeEqualTo behandlerRef
                }
                it("getBehandlere lagrer ny behandler for arbeidstaker med annen fastlege enn siste lagret behandler og det finnes tidligere lagret behandler lik fastlege") {
                    val fastlegeResponse = generateFastlegeResponse(parentHerId)
                    val otherFastlegeResponse = generateFastlegeResponse(otherParentHerId)
                    val behandlerRef = behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_FNR,
                        fastlegeResponse.toBehandler(partnerId),
                    ).behandlerRef
                    val otherBehandlerRef = behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_FNR,
                        otherFastlegeResponse.toBehandler(otherPartnerId),
                    ).behandlerRef
                    fastlegeClientMock.mockResponse(
                        UserConstants.ARBEIDSTAKER_FNR,
                        fastlegeResponse,
                    )
                    partnerinfoClientMock.mockResponse(parentHerId, generatePartnerinfoResponse(partnerId))

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo partnerId
                    }

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 3
                    behandlerDialogmeldingForArbeidstakerList[0].behandlerRef shouldBeEqualTo behandlerRef
                    behandlerDialogmeldingForArbeidstakerList[1].behandlerRef shouldBeEqualTo otherBehandlerRef
                    behandlerDialogmeldingForArbeidstakerList[2].behandlerRef shouldBeEqualTo behandlerRef
                }
            }
        }
    }
})
