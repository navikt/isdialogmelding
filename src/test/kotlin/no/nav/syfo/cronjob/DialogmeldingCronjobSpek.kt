package no.nav.syfo.cronjob

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.database.getBestilling
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.DialogmeldingService
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Assert
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

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment,
            )

            val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
                database = database,
                pdlClient = pdlClient,
            )

            val dialogmeldingService = DialogmeldingService(
                pdlClient = pdlClient,
                mqSender = mqSenderMock,
            )

            val dialogmeldingSendCronjob = DialogmeldingSendCronjob(
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                dialogmeldingService = dialogmeldingService,
            )

            beforeEachTest {
                database.dropData()
                clearAllMocks()
                justRun { mqSenderMock.sendMessageToEmottak(any()) }
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
                        connection.getBestilling(
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
                        connection.getBestilling(
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
                it("Sender bestilt dialogmelding uten relasjon mellom arbeidstaker og behandler") {
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
                        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR,
                    )
                    dialogmeldingToBehandlerService.handleIncomingDialogmeldingBestilling(dialogmeldingBestillingDTO)

                    val pBehandlerDialogmeldingBestillingBefore = database.connection.use { connection ->
                        connection.getBestilling(
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
                        connection.getBestilling(
                            uuid = dialogmeldingBestillingUuid,
                        )
                    }
                    pBehandlerDialogmeldingBestillingAfter!!.sendt shouldNotBeEqualTo null
                    pBehandlerDialogmeldingBestillingAfter.sendtTries shouldBeEqualTo 1
                }
                it("Sender bestilt oppfølgingsplan") {
                    clearAllMocks()
                    val messageSlot = slot<String>()
                    justRun { mqSenderMock.sendMessageToEmottak(capture(messageSlot)) }
                    val behandlerRef = UUID.randomUUID()
                    val partnerId = PartnerId(1)
                    val behandler = generateBehandler(
                        behandlerRef = behandlerRef,
                        partnerId = partnerId,
                        kontornavn = "navn",
                    )
                    database.createBehandlerForArbeidstaker(
                        behandler = behandler,
                        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR_OPPFOLGINGSPLAN,
                    )

                    val dialogmeldingBestillingUuid = UUID.randomUUID()

                    val dialogmeldingBestillingDTO = generateDialogmeldingToBehandlerBestillingOppfolgingsplanDTO(
                        uuid = dialogmeldingBestillingUuid,
                        behandlerRef = behandlerRef,
                        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR_OPPFOLGINGSPLAN,
                    )
                    dialogmeldingToBehandlerService.handleIncomingDialogmeldingBestilling(dialogmeldingBestillingDTO)

                    val pBehandlerDialogmeldingBestillingBefore = database.connection.use { connection ->
                        connection.getBestilling(
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
                    val expectedFellesformatMessageAsRegex = defaultFellesformatMessageXmlRegex()
                    val actualFellesformatMessage = messageSlot.captured

                    Assert.assertTrue(
                        expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
                    )

                    val pBehandlerDialogmeldingBestillingAfter = database.connection.use { connection ->
                        connection.getBestilling(
                            uuid = dialogmeldingBestillingUuid,
                        )
                    }
                    pBehandlerDialogmeldingBestillingAfter!!.sendt shouldNotBeEqualTo null
                    pBehandlerDialogmeldingBestillingAfter.sendtTries shouldBeEqualTo 1
                }
            }
        }
    }
})
