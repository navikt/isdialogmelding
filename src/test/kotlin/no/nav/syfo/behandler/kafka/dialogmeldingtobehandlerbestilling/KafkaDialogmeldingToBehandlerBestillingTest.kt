package no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.bestilling.database.getBestilling
import no.nav.syfo.dialogmelding.bestilling.kafka.DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC
import no.nav.syfo.dialogmelding.bestilling.kafka.DialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.dialogmelding.bestilling.kafka.pollAndProcessDialogmeldingBestilling
import no.nav.syfo.dialogmelding.status.database.getDialogmeldingStatusNotPublished
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.testhelper.testdata.lagreBehandler
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class KafkaDialogmeldingToBehandlerBestillingTest {
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
    private val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
        database = database,
        pdlClient = pdlClient,
    )
    private val partition = 0
    private val dialogmeldingToBehandlerBestillingTopicPartition = TopicPartition(
        DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
        partition,
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {
        @Test
        fun `should persist incoming bestillinger`() {
            val behandler = lagreBehandler(database)
            val dialogmeldingBestillingUuid = UUID.randomUUID()
            val dialogmeldingBestilling = generateDialogmeldingToBehandlerBestillingDTO(
                uuid = dialogmeldingBestillingUuid,
                behandlerRef = behandler.behandlerRef,
            )
            val dialogmeldingBestillingRecord = ConsumerRecord(
                DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
                partition,
                1,
                dialogmeldingBestilling.dialogmeldingUuid,
                dialogmeldingBestilling,
            )
            val mockConsumer = mockk<KafkaConsumer<String, DialogmeldingToBehandlerBestillingDTO>>()
            every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmeldingToBehandlerBestillingTopicPartition to listOf(
                        dialogmeldingBestillingRecord,
                    )
                )
            )
            every { mockConsumer.commitSync() } returns Unit

            pollAndProcessDialogmeldingBestilling(
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
            )

            verify(exactly = 1) { mockConsumer.commitSync() }

            val pBehandlerDialogmeldingBestilling =
                database.getBestilling(uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid))
            assertNotNull(pBehandlerDialogmeldingBestilling)
            assertEquals(dialogmeldingBestillingUuid, pBehandlerDialogmeldingBestilling!!.uuid)
            assertEquals(dialogmeldingBestilling.dialogmeldingTekst, pBehandlerDialogmeldingBestilling.tekst)
            assertEquals("SYFO", pBehandlerDialogmeldingBestilling.kilde)
        }

        @Test
        fun `should persist incoming bestilling with no kilde`() {
            val behandler = lagreBehandler(database)
            val dialogmeldingBestillingUuid = UUID.randomUUID()
            val dialogmeldingBestilling = generateDialogmeldingToBehandlerBestillingDTO(
                uuid = dialogmeldingBestillingUuid,
                behandlerRef = behandler.behandlerRef,
            ).copy(
                kilde = null,
            )
            val dialogmeldingBestillingRecord = ConsumerRecord(
                DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
                partition,
                1,
                dialogmeldingBestilling.dialogmeldingUuid,
                dialogmeldingBestilling,
            )
            val mockConsumer = mockk<KafkaConsumer<String, DialogmeldingToBehandlerBestillingDTO>>()
            every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmeldingToBehandlerBestillingTopicPartition to listOf(
                        dialogmeldingBestillingRecord,
                    )
                )
            )
            every { mockConsumer.commitSync() } returns Unit

            pollAndProcessDialogmeldingBestilling(
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
            )

            verify(exactly = 1) { mockConsumer.commitSync() }

            val pBehandlerDialogmeldingBestilling =
                database.getBestilling(uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid))
            assertNotNull(pBehandlerDialogmeldingBestilling)
            assertEquals(dialogmeldingBestillingUuid, pBehandlerDialogmeldingBestilling!!.uuid)
            assertEquals(dialogmeldingBestilling.dialogmeldingTekst, pBehandlerDialogmeldingBestilling.tekst)
            assertNull(pBehandlerDialogmeldingBestilling.kilde)
        }

        @Test
        fun `persists dialogmelding-status BESTILT when incoming bestilling`() {
            val behandler = lagreBehandler(database)
            val dialogmeldingBestilling = generateDialogmeldingToBehandlerBestillingDTO(
                uuid = UUID.randomUUID(),
                behandlerRef = behandler.behandlerRef,
            )
            val dialogmeldingBestillingRecord = ConsumerRecord(
                DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
                partition,
                1,
                dialogmeldingBestilling.dialogmeldingUuid,
                dialogmeldingBestilling,
            )
            val mockConsumer = mockk<KafkaConsumer<String, DialogmeldingToBehandlerBestillingDTO>>()
            every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmeldingToBehandlerBestillingTopicPartition to listOf(
                        dialogmeldingBestillingRecord,
                    )
                )
            )
            every { mockConsumer.commitSync() } returns Unit

            pollAndProcessDialogmeldingBestilling(
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
            )

            val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
            assertEquals(1, dialogmeldingStatusNotPublished.size)

            val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
            val pBehandlerDialogmeldingBestilling =
                database.getBestilling(
                    uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid)
                )
            assertEquals(DialogmeldingStatusType.BESTILT.name, pDialogmeldingStatus.status)
            assertNull(pDialogmeldingStatus.tekst)
            assertEquals(pBehandlerDialogmeldingBestilling?.id, pDialogmeldingStatus.bestillingId)
            assertNotNull(pDialogmeldingStatus.createdAt)
            assertNotNull(pDialogmeldingStatus.updatedAt)
            assertNull(pDialogmeldingStatus.publishedAt)
        }
    }

    @Nested
    @DisplayName("Should only persist once when duplicates")
    inner class ShouldOnlyPersistOnceWhenDuplicates {
        @Test
        fun `Should only persist once when duplicates`() {
            val behandler = lagreBehandler(database)
            val dialogmeldingBestillingUuid = UUID.randomUUID()
            val dialogmeldingBestilling = generateDialogmeldingToBehandlerBestillingDTO(
                uuid = dialogmeldingBestillingUuid,
                behandlerRef = behandler.behandlerRef,
            )
            val dialogmeldingBestillingRecord = ConsumerRecord(
                DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
                partition,
                2,
                dialogmeldingBestilling.dialogmeldingUuid,
                dialogmeldingBestilling,
            )
            val dialogmeldingBestillingRecordDuplicate = ConsumerRecord(
                DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
                partition,
                3,
                dialogmeldingBestilling.dialogmeldingUuid,
                dialogmeldingBestilling,
            )
            val mockConsumer = mockk<KafkaConsumer<String, DialogmeldingToBehandlerBestillingDTO>>()
            every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmeldingToBehandlerBestillingTopicPartition to listOf(
                        dialogmeldingBestillingRecord,
                        dialogmeldingBestillingRecordDuplicate,
                    )
                )
            )
            every { mockConsumer.commitSync() } returns Unit

            pollAndProcessDialogmeldingBestilling(
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
            )

            verify(exactly = 1) { mockConsumer.commitSync() }

            val pBehandlerDialogmeldingBestilling =
                database.getBestilling(uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid))

            assertNotNull(pBehandlerDialogmeldingBestilling)
            assertEquals(dialogmeldingBestillingUuid, pBehandlerDialogmeldingBestilling!!.uuid)
        }
    }

    @Nested
    @DisplayName("Does not persist when behandlerRef not valid")
    inner class DoesNotPersistWhenBehandlerRefNotValid {
        @Test
        fun `should not persist incoming bestillinger when behandlerRef is invalid`() {
            val dialogmeldingBestillingUuid = UUID.randomUUID()
            val dialogmeldingBestilling = generateDialogmeldingToBehandlerBestillingDTO(
                uuid = dialogmeldingBestillingUuid,
                behandlerRef = UUID.randomUUID(),
            )
            val dialogmeldingBestillingRecord = ConsumerRecord(
                DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
                partition,
                1,
                dialogmeldingBestilling.dialogmeldingUuid,
                dialogmeldingBestilling,
            )
            val mockConsumer = mockk<KafkaConsumer<String, DialogmeldingToBehandlerBestillingDTO>>()
            every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmeldingToBehandlerBestillingTopicPartition to listOf(
                        dialogmeldingBestillingRecord,
                    )
                )
            )
            every { mockConsumer.commitSync() } returns Unit

            pollAndProcessDialogmeldingBestilling(
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
            )

            verify(exactly = 1) { mockConsumer.commitSync() }
            val pBehandlerDialogmeldingBestilling =
                database.getBestilling(uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid))
            assertNull(pBehandlerDialogmeldingBestilling)
        }
    }
}
