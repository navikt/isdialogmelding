package no.nav.syfo.cronjob

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.database.getBestillinger
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.DialogmeldingService
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.Random
import java.util.UUID

class DialogmeldingCronjobSpek : Spek({

    describe(DialogmeldingSendCronjob::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val random = Random()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val environment = externalMockEnvironment.environment
            val pdlClient = PdlClient(
                azureAdClient = AzureAdClient(
                    azureAppClientId = environment.aadAppClient,
                    azureAppClientSecret = environment.azureAppClientSecret,
                    azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
                ),
                pdlClientId = environment.pdlClientId,
                pdlUrl = environment.pdlUrl,
            )
            val mqSenderMock = mockk<MQSender>()
            justRun { mqSenderMock.sendMessageToEmottak(any()) }

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment,
            )

            val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
                database = database,
                pdlClient = pdlClient,
            )

            val dialogmeldingService = DialogmeldingService(
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                mqSender = mqSenderMock,
            )

            val dialogmeldingSendCronjob = DialogmeldingSendCronjob(
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                dialogmeldingService = dialogmeldingService,
                mqSender = mqSenderMock,
            )

            beforeEachTest {
                database.dropData()
            }
            afterEachTest {
                database.dropData()
            }

            describe("Cronjob sender bestilte dialogmeldinger") {
                it("Sender bestilt dialogmelding") {
                    val behandlerRef = UUID.randomUUID()
                    val partnerId = PartnerId(random.nextInt())
                    val behandler = generateBehandler(behandlerRef, partnerId)
                    database.createBehandlerForArbeidstaker(
                        behandler = behandler,
                        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                    )

                    val dialogmeldingBestillingUuid = UUID.randomUUID()

                    val dialogmeldingBestillingDTO = generateDialogmeldingToBehandlerBestillingDTO(
                        uuid = dialogmeldingBestillingUuid,
                        behandlerRef = behandlerRef,
                        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                    )
                    dialogmeldingToBehandlerService.handleIncomingDialogmeldingBestilling(dialogmeldingBestillingDTO)

                    val pBehandlerDialogmeldingBestillingBefore = database.connection.use { connection ->
                        connection.getBestillinger(
                            uuid = dialogmeldingBestillingUuid,
                        )
                    }
                    pBehandlerDialogmeldingBestillingBefore shouldNotBeEqualTo null
                    pBehandlerDialogmeldingBestillingBefore!!.sendt shouldBeEqualTo null
                    pBehandlerDialogmeldingBestillingBefore.sendtTries shouldBeEqualTo 0

                    runBlocking {
                        val result = dialogmeldingSendCronjob.dialogmeldingSendJob()
                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }
                    verify(exactly = 1) { mqSenderMock.sendMessageToEmottak(any()) }

                    val pBehandlerDialogmeldingBestillingAfter = database.connection.use { connection ->
                        connection.getBestillinger(
                            uuid = dialogmeldingBestillingUuid,
                        )
                    }
                    pBehandlerDialogmeldingBestillingAfter!!.sendt shouldNotBeEqualTo null
                    pBehandlerDialogmeldingBestillingAfter.sendtTries shouldBeEqualTo 1

                    clearMocks(mqSenderMock)
                    runBlocking {
                        val result = dialogmeldingSendCronjob.dialogmeldingSendJob()
                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                    verify(exactly = 0) { mqSenderMock.sendMessageToEmottak(any()) }
                }
            }
        }
    }
})
