package no.nav.syfo.behandler

import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.database.getBehandlerDialogmeldingForArbeidstaker
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class BehandlerServiceSpek : Spek({

    val anyToken = "token"
    val anyCallId = "callId"

    describe("BehandlerService") {
        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment()
            val azureAdV2Client = AzureAdClient(
                azureAppClientId = externalMockEnvironment.environment.aadAppClient,
                azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
                azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
            )
            val database = externalMockEnvironment.database
            val fastlegeClient = FastlegeClient(
                azureAdClient = azureAdV2Client,
                fastlegeRestClientId = externalMockEnvironment.environment.fastlegeRestClientId,
                fastlegeRestUrl = externalMockEnvironment.environment.fastlegeRestUrl,
            )
            val partnerinfoClient = PartnerinfoClient(
                azureAdClient = azureAdV2Client,
                syfoPartnerinfoClientId = externalMockEnvironment.environment.syfoPartnerinfoClientId,
                syfoPartnerinfoUrl = externalMockEnvironment.environment.syfoPartnerinfoUrl,
            )
            val behandlerService = BehandlerService(
                fastlegeClient = fastlegeClient,
                partnerinfoClient = partnerinfoClient,
                database = database,
            )

            beforeGroup {
                externalMockEnvironment.startExternalMocks()
            }

            afterEachTest {
                database.dropData()
            }

            afterGroup {
                externalMockEnvironment.stopExternalMocks()
            }

            describe("getBehandlere uten behandlere i database") {
                it("getBehandlere lagrer behandler for arbeidstaker med fastlege") {
                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("getBehandlere kallt flere ganger lagrer én behandler for arbeidstaker med fastlege") {

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("getBehandlere lagrer én behandler for to arbeidstakere med samme fastlege") {
                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
                    }

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 1

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ANNEN_ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
                    }

                    val behandlerDialogmeldingForAnnenArbeidstakerList =
                        database.getBehandlerDialogmeldingForArbeidstaker(
                            UserConstants.ANNEN_ARBEIDSTAKER_FNR,
                        )
                    behandlerDialogmeldingForAnnenArbeidstakerList.size shouldBeEqualTo 1
                    behandlerDialogmeldingForArbeidstakerList.first() shouldBeEqualTo behandlerDialogmeldingForAnnenArbeidstakerList.first()
                }
            }
            describe("getBehandlere med behandlere i databasen") {
                it("getBehandlere lagrer ikke ny behandler når fastlege er siste lagret behandler for arbeidstaker") {
                    val fastlegeResponse = generateFastlegeResponse(UserConstants.HERID)
                    behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_FNR,
                        fastlegeResponse.toBehandler(UserConstants.PARTNERID),
                    )
                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("getBehandlere lagrer ny kobling til behandler for arbeidstaker når samme behandler finnes for annen arbeidstaker") {
                    val fastlegeResponse = generateFastlegeResponse(UserConstants.HERID)
                    behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_ANNEN_FASTLEGE_HERID_FNR,
                        fastlegeResponse.toBehandler(UserConstants.PARTNERID),
                    )

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
                    }

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("getBehandlere lagrer ny behandler for arbeidstaker når fastlege er annen enn siste lagret behandler") {
                    val behandlerRef = behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_FNR,
                        generateFastlegeResponse(UserConstants.OTHER_HERID).toBehandler(UserConstants.OTHER_PARTNERID),
                    ).behandlerRef

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
                    }

                    val behandlerDialogmeldingForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    behandlerDialogmeldingForArbeidstakerList.size shouldBeEqualTo 2
                    behandlerDialogmeldingForArbeidstakerList[0].behandlerRef shouldNotBeEqualTo behandlerRef
                    behandlerDialogmeldingForArbeidstakerList[1].behandlerRef shouldBeEqualTo behandlerRef
                }

                it("getBehandlere lagrer ny behandler for arbeidstaker med annen fastlege enn siste lagret behandler og det finnes tidligere lagret behandler lik fastlege") {
                    val behandlerRef = behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_FNR,
                        generateFastlegeResponse(UserConstants.HERID).toBehandler(UserConstants.PARTNERID),
                    ).behandlerRef
                    val otherBehandlerRef = behandlerService.createBehandlerDialogmelding(
                        UserConstants.ARBEIDSTAKER_FNR,
                        generateFastlegeResponse(UserConstants.OTHER_HERID).toBehandler(UserConstants.OTHER_PARTNERID),
                    ).behandlerRef

                    runBlocking {
                        val behandlere =
                            behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, anyToken, anyCallId)
                        behandlere.size shouldBeEqualTo 1
                        behandlere.first().partnerId shouldBeEqualTo UserConstants.PARTNERID
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
