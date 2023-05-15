package no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.bestilling.database.getBestilling
import no.nav.syfo.dialogmelding.bestilling.kafka.*
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService
import no.nav.syfo.dialogmelding.status.database.getDialogmeldingStatusNotPublished
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.testhelper.testdata.lagreBehandler
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.util.UUID

class KafkaDialogmeldingToBehandlerBestillingSpek : Spek({

    with(TestApplicationEngine()) {
        start()
        val database = ExternalMockEnvironment.instance.database
        val environment = ExternalMockEnvironment.instance.environment
        val pdlClient = PdlClient(
            azureAdClient = AzureAdClient(
                azureAppClientId = environment.aadAppClient,
                azureAppClientSecret = environment.azureAppClientSecret,
                azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
            ),
            pdlClientId = environment.pdlClientId,
            pdlUrl = environment.pdlUrl,
        )
        val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
            database = database,
            pdlClient = pdlClient,
            dialogmeldingStatusService = DialogmeldingStatusService(
                database = database,
            ),
        )

        afterEachTest {
            database.dropData()
        }

        describe(KafkaDialogmeldingToBehandlerBestillingSpek::class.java.simpleName) {

            describe("Motta dialogmelding bestillinger") {
                val partition = 0
                val dialogmeldingToBehandlerBestillingTopicPartition = TopicPartition(
                    DIALOGMELDING_TO_BEHANDLER_BESTILLING_TOPIC,
                    partition,
                )
                describe("Happy path") {
                    it("should persist incoming bestillinger") {
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

                        runBlocking {
                            pollAndProcessDialogmeldingBestilling(
                                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
                            )
                        }

                        verify(exactly = 1) { mockConsumer.commitSync() }

                        val pBehandlerDialogmeldingBestilling = database.getBestilling(uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid))
                        pBehandlerDialogmeldingBestilling shouldNotBeEqualTo null
                        pBehandlerDialogmeldingBestilling!!.uuid shouldBeEqualTo dialogmeldingBestillingUuid
                        pBehandlerDialogmeldingBestilling.tekst!! shouldBeEqualTo dialogmeldingBestilling.dialogmeldingTekst
                    }
                    it("persists dialogmelding-status BESTILT when incoming bestilling") {
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

                        runBlocking {
                            pollAndProcessDialogmeldingBestilling(
                                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
                            )
                        }

                        val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
                        dialogmeldingStatusNotPublished.size shouldBeEqualTo 1

                        val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
                        val pBehandlerDialogmeldingBestilling =
                            database.getBestilling(
                                uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid)
                            )
                        pDialogmeldingStatus.status shouldBeEqualTo DialogmeldingStatusType.BESTILT.name
                        pDialogmeldingStatus.tekst.shouldBeNull()
                        pDialogmeldingStatus.bestillingId shouldBeEqualTo pBehandlerDialogmeldingBestilling?.id
                        pDialogmeldingStatus.createdAt.shouldNotBeNull()
                        pDialogmeldingStatus.updatedAt.shouldNotBeNull()
                        pDialogmeldingStatus.publishedAt.shouldBeNull()
                    }
                }
                describe("Should only persist once when duplicates") {
                    it("Should only persist once when duplicates") {
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

                        runBlocking {
                            pollAndProcessDialogmeldingBestilling(
                                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
                            )
                        }

                        verify(exactly = 1) { mockConsumer.commitSync() }

                        val pBehandlerDialogmeldingBestilling = database.getBestilling(uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid))

                        pBehandlerDialogmeldingBestilling shouldNotBeEqualTo null
                        pBehandlerDialogmeldingBestilling!!.uuid shouldBeEqualTo dialogmeldingBestillingUuid
                    }
                }
                describe("Does not persist when behandlerRef not valid") {
                    it("should not persist incoming bestillinger when behandlerRef is invalid") {
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

                        runBlocking {
                            pollAndProcessDialogmeldingBestilling(
                                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                                kafkaConsumerDialogmeldingToBehandlerBestilling = mockConsumer,
                            )
                        }

                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val pBehandlerDialogmeldingBestilling = database.getBestilling(uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid))
                        pBehandlerDialogmeldingBestilling shouldBeEqualTo null
                    }
                }
            }
        }
    }
})
