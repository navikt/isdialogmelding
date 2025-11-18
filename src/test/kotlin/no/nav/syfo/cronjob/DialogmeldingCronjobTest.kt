package no.nav.syfo.cronjob

import io.mockk.*
import kotlinx.coroutines.test.runTest
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
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class DialogmeldingCronjobTest {

    private val random = Random()
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = externalMockEnvironment.environment
    private val pdlClient = PdlClient(
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
    private val mqSenderMock = mockk<MQSender>()

    private val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
        database = database,
        pdlClient = pdlClient,
    )
    private val dialogmeldingStatusService = DialogmeldingStatusService(
        database = database,
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        dialogmeldingStatusProducer = DialogmeldingStatusProducer(
            kafkaProducer = mockk<KafkaProducer<String, KafkaDialogmeldingStatusDTO>>()
        )
    )
    private val dialogmeldingService = DialogmeldingService(
        pdlClient = pdlClient,
        mqSender = mqSenderMock,
    )
    private val dialogmeldingSendCronjob = DialogmeldingSendCronjob(
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        dialogmeldingService = dialogmeldingService,
        dialogmeldingStatusService = dialogmeldingStatusService,
    )

    @BeforeEach
    fun beforeEach() {
        database.dropData()
        clearAllMocks()
        justRun { mqSenderMock.sendMessageToEmottak(any()) }
    }

    @Test
    fun `Sender bestilt dialogmelding`() = runTest {
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
        assertNotNull(pBehandlerDialogmeldingBestillingBefore)
        assertNull(pBehandlerDialogmeldingBestillingBefore!!.sendt)
        assertEquals(0, pBehandlerDialogmeldingBestillingBefore.sendtTries)

        val result = dialogmeldingSendCronjob.dialogmeldingSendJob()
        assertEquals(0, result.failed)
        assertEquals(1, result.updated)
        verify(exactly = 1) { mqSenderMock.sendMessageToEmottak(any()) }

        val pBehandlerDialogmeldingBestillingAfter =
            database.getBestilling(uuid = dialogmeldingBestillingUuid)
        assertNotNull(pBehandlerDialogmeldingBestillingAfter!!.sendt)
        assertEquals(1, pBehandlerDialogmeldingBestillingAfter.sendtTries)

        clearMocks(mqSenderMock)
        val result2 = dialogmeldingSendCronjob.dialogmeldingSendJob()
        assertEquals(0, result2.failed)
        assertEquals(0, result2.updated)
        verify(exactly = 0) { mqSenderMock.sendMessageToEmottak(any()) }
    }

    @Test
    fun `Sender bestilt dialogmelding uten relasjon mellom arbeidstaker og behandler`() = runTest {
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
        assertNotNull(pBehandlerDialogmeldingBestillingBefore)
        assertNull(pBehandlerDialogmeldingBestillingBefore!!.sendt)
        assertEquals(0, pBehandlerDialogmeldingBestillingBefore.sendtTries)

        val result = dialogmeldingSendCronjob.dialogmeldingSendJob()
        assertEquals(0, result.failed)
        assertEquals(1, result.updated)
        verify(exactly = 1) { mqSenderMock.sendMessageToEmottak(any()) }

        val pBehandlerDialogmeldingBestillingAfter =
            database.getBestilling(uuid = dialogmeldingBestillingUuid)
        assertNotNull(pBehandlerDialogmeldingBestillingAfter!!.sendt)
        assertEquals(1, pBehandlerDialogmeldingBestillingAfter.sendtTries)
    }

    @Test
    fun `Sender bestilt oppfølgingsplan`() = runTest {
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
        assertNotNull(pBehandlerDialogmeldingBestillingBefore)
        assertNull(pBehandlerDialogmeldingBestillingBefore!!.sendt)
        assertEquals(0, pBehandlerDialogmeldingBestillingBefore.sendtTries)

        val result = dialogmeldingSendCronjob.dialogmeldingSendJob()
        assertEquals(0, result.failed)
        assertEquals(1, result.updated)
        verify(exactly = 1) { mqSenderMock.sendMessageToEmottak(any()) }
        val expectedFellesformatMessageAsRegex = defaultFellesformatMessageXmlRegex()
        val actualFellesformatMessage = messageSlot.captured

        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )

        val pBehandlerDialogmeldingBestillingAfter =
            database.getBestilling(uuid = dialogmeldingBestillingUuid)
        assertNotNull(pBehandlerDialogmeldingBestillingAfter!!.sendt)
        assertEquals(1, pBehandlerDialogmeldingBestillingAfter.sendtTries)
    }

    @Test
    fun `Sending av bestilt dialogmelding lagrer dialogmelding-status SENDT`() = runTest {
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

        dialogmeldingSendCronjob.dialogmeldingSendJob()

        val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
        assertEquals(1, dialogmeldingStatusNotPublished.size)

        val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
        assertEquals(DialogmeldingStatusType.SENDT.name, pDialogmeldingStatus.status)
        assertNull(pDialogmeldingStatus.tekst)
        assertEquals(pBehandlerDialogmeldingBestilling?.id, pDialogmeldingStatus.bestillingId)
        assertNotNull(pDialogmeldingStatus.createdAt)
        assertNotNull(pDialogmeldingStatus.updatedAt)
        assertNull(pDialogmeldingStatus.publishedAt)
    }
}
