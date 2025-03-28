package no.nav.syfo.cronjob

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.DialogmeldingService
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.bestilling.database.getBestilling
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService
import no.nav.syfo.dialogmelding.status.database.getDialogmeldingStatusNotPublished
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.dialogmelding.status.kafka.DialogmeldingStatusProducer
import no.nav.syfo.dialogmelding.status.kafka.KafkaDialogmeldingStatusDTO
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.createBehandlerForArbeidstaker
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.defaultFellesformatMessageXmlRegex
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingOppfolgingsplanDTO
import no.nav.syfo.testhelper.testdata.lagreDialogmeldingBestilling
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.Assertions.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class DialogmeldingCronjobSpek : Spek({

    describe(DialogmeldingSendCronjob::class.java.simpleName) {
        val random = Random()
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val environment = externalMockEnvironment.environment
        val pdlClient = PdlClient(
            azureAdClient = AzureAdClient(
                azureAppClientId = environment.aadAppClient,
                azureAppClientSecret = environment.azureAppClientSecret,
                azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
                httpClient = externalMockEnvironment.mockHttpClient,
            ),
            pdlClientId = environment.pdlClientId,
            pdlUrl = environment.pdlUrl,
            httpClient = externalMockEnvironment.mockHttpClient,
        )
        val mqSenderMock = mockk<MQSender>()

        val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
            database = database,
            pdlClient = pdlClient,
        )
        val dialogmeldingStatusService = DialogmeldingStatusService(
            database = database,
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
            dialogmeldingStatusProducer = DialogmeldingStatusProducer(
                kafkaProducer = mockk<KafkaProducer<String, KafkaDialogmeldingStatusDTO>>()
            )
        )
        val dialogmeldingService = DialogmeldingService(
            pdlClient = pdlClient,
            mqSender = mqSenderMock,
        )
        val dialogmeldingSendCronjob = DialogmeldingSendCronjob(
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
            dialogmeldingService = dialogmeldingService,
            dialogmeldingStatusService = dialogmeldingStatusService,
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
                lagreDialogmeldingBestilling(
                    database = database,
                    behandler = behandler,
                    dialogmeldingToBehandlerBestillingDTO = dialogmeldingBestillingDTO,
                )

                val pBehandlerDialogmeldingBestillingBefore =
                    database.getBestilling(uuid = dialogmeldingBestillingUuid)
                pBehandlerDialogmeldingBestillingBefore shouldNotBeEqualTo null
                pBehandlerDialogmeldingBestillingBefore!!.sendt shouldBeEqualTo null
                pBehandlerDialogmeldingBestillingBefore.sendtTries shouldBeEqualTo 0

                runBlocking {
                    val result = dialogmeldingSendCronjob.dialogmeldingSendJob()
                    result.failed shouldBeEqualTo 0
                    result.updated shouldBeEqualTo 1
                }
                verify(exactly = 1) { mqSenderMock.sendMessageToEmottak(any()) }

                val pBehandlerDialogmeldingBestillingAfter =
                    database.getBestilling(uuid = dialogmeldingBestillingUuid)
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
                lagreDialogmeldingBestilling(
                    database = database,
                    behandler = behandler,
                    dialogmeldingToBehandlerBestillingDTO = dialogmeldingBestillingDTO,
                )

                val pBehandlerDialogmeldingBestillingBefore =
                    database.getBestilling(uuid = dialogmeldingBestillingUuid)
                pBehandlerDialogmeldingBestillingBefore shouldNotBeEqualTo null
                pBehandlerDialogmeldingBestillingBefore!!.sendt shouldBeEqualTo null
                pBehandlerDialogmeldingBestillingBefore.sendtTries shouldBeEqualTo 0

                runBlocking {
                    val result = dialogmeldingSendCronjob.dialogmeldingSendJob()
                    result.failed shouldBeEqualTo 0
                    result.updated shouldBeEqualTo 1
                }
                verify(exactly = 1) { mqSenderMock.sendMessageToEmottak(any()) }

                val pBehandlerDialogmeldingBestillingAfter =
                    database.getBestilling(uuid = dialogmeldingBestillingUuid)
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
                lagreDialogmeldingBestilling(
                    database = database,
                    behandler = behandler,
                    dialogmeldingToBehandlerBestillingDTO = dialogmeldingBestillingDTO
                )

                val pBehandlerDialogmeldingBestillingBefore =
                    database.getBestilling(uuid = dialogmeldingBestillingUuid)
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

                assertTrue(
                    expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
                )

                val pBehandlerDialogmeldingBestillingAfter =
                    database.getBestilling(uuid = dialogmeldingBestillingUuid)
                pBehandlerDialogmeldingBestillingAfter!!.sendt shouldNotBeEqualTo null
                pBehandlerDialogmeldingBestillingAfter.sendtTries shouldBeEqualTo 1
            }
            it("Sending av bestilt dialogmelding lagrer dialogmelding-status SENDT") {
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
                lagreDialogmeldingBestilling(
                    database = database,
                    behandler = behandler,
                    dialogmeldingToBehandlerBestillingDTO = dialogmeldingBestillingDTO
                )
                val pBehandlerDialogmeldingBestilling = database.getBestilling(uuid = dialogmeldingBestillingUuid)

                runBlocking {
                    dialogmeldingSendCronjob.dialogmeldingSendJob()
                }

                val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
                dialogmeldingStatusNotPublished.size shouldBeEqualTo 1

                val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
                pDialogmeldingStatus.status shouldBeEqualTo DialogmeldingStatusType.SENDT.name
                pDialogmeldingStatus.tekst.shouldBeNull()
                pDialogmeldingStatus.bestillingId shouldBeEqualTo pBehandlerDialogmeldingBestilling?.id
                pDialogmeldingStatus.createdAt.shouldNotBeNull()
                pDialogmeldingStatus.updatedAt.shouldNotBeNull()
                pDialogmeldingStatus.publishedAt.shouldBeNull()
            }
        }
    }
})
