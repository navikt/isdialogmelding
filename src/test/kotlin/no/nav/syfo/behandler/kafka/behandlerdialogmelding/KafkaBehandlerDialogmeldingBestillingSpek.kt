package no.nav.syfo.behandler.kafka.behandlerdialogmelding

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.behandler.database.createBehandlerDialogmelding
import no.nav.syfo.behandler.database.getBehandlerDialogmeldingBestilling
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateBehandlerDialogmeldingBestillingDTO
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.util.Random
import java.util.UUID

class KafkaBehandlerDialogmeldingBestillingSpek : Spek({

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
        val behandlerDialogmeldingService = BehandlerDialogmeldingService(
            database = database,
            pdlClient = pdlClient,
        )
        val random = Random()

        afterEachTest {
            database.dropData()
        }

        describe(KafkaBehandlerDialogmeldingBestillingSpek::class.java.simpleName) {

            describe("Motta dialogmelding bestillinger") {
                val partition = 0
                val behandlerDialogmeldingBestillingTopicPartition = TopicPartition(
                    DIALOGMELDING_BESTILLING_TOPIC,
                    partition,
                )
                describe("Happy path") {
                    it("should persist incoming bestillinger") {
                        val behandlerRef = UUID.randomUUID()
                        val partnerId = random.nextInt()
                        val behandler = generateBehandler(behandlerRef, partnerId)
                        database.connection.use {
                            it.createBehandlerDialogmelding(behandler)
                            it.commit()
                        }

                        val dialogmeldingBestillingUuid = UUID.randomUUID()
                        val dialogmeldingBestilling = generateBehandlerDialogmeldingBestillingDTO(
                            uuid = dialogmeldingBestillingUuid,
                            behandlerRef = behandlerRef,
                        )
                        val dialogmeldingBestillingRecord = ConsumerRecord(
                            DIALOGMELDING_BESTILLING_TOPIC,
                            partition,
                            1,
                            dialogmeldingBestilling.dialogmeldingUuid,
                            dialogmeldingBestilling,
                        )
                        val mockConsumer = mockk<KafkaConsumer<String, BehandlerDialogmeldingBestillingDTO>>()
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                behandlerDialogmeldingBestillingTopicPartition to listOf(
                                    dialogmeldingBestillingRecord,
                                )
                            )
                        )
                        every { mockConsumer.commitSync() } returns Unit

                        runBlocking {
                            pollAndProcessDialogmeldingBestilling(
                                behandlerDialogmeldingService = behandlerDialogmeldingService,
                                kafkaConsumerDialogmeldingBestilling = mockConsumer,
                            )
                        }

                        verify(exactly = 1) { mockConsumer.commitSync() }

                        val pBehandlerDialogmeldingBestilling =
                            database.connection.use {
                                it.getBehandlerDialogmeldingBestilling(
                                    uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid)
                                )
                            }
                        pBehandlerDialogmeldingBestilling shouldNotBeEqualTo null
                        pBehandlerDialogmeldingBestilling!!.uuid shouldBeEqualTo dialogmeldingBestillingUuid
                        pBehandlerDialogmeldingBestilling.tekst!! shouldBeEqualTo dialogmeldingBestilling.dialogmeldingTekst
                    }
                }
                describe("Should only persist once when duplicates") {
                    it("Should only persist once when duplicates") {
                        val behandlerRef = UUID.randomUUID()
                        val partnerId = random.nextInt()
                        val behandler = generateBehandler(behandlerRef, partnerId)
                        database.connection.use {
                            it.createBehandlerDialogmelding(behandler)
                            it.commit()
                        }

                        val dialogmeldingBestillingUuid = UUID.randomUUID()
                        val dialogmeldingBestilling = generateBehandlerDialogmeldingBestillingDTO(
                            uuid = dialogmeldingBestillingUuid,
                            behandlerRef = behandlerRef,
                        )
                        val dialogmeldingBestillingRecord = ConsumerRecord(
                            DIALOGMELDING_BESTILLING_TOPIC,
                            partition,
                            2,
                            dialogmeldingBestilling.dialogmeldingUuid,
                            dialogmeldingBestilling,
                        )
                        val dialogmeldingBestillingRecordDuplicate = ConsumerRecord(
                            DIALOGMELDING_BESTILLING_TOPIC,
                            partition,
                            3,
                            dialogmeldingBestilling.dialogmeldingUuid,
                            dialogmeldingBestilling,
                        )
                        val mockConsumer = mockk<KafkaConsumer<String, BehandlerDialogmeldingBestillingDTO>>()
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                behandlerDialogmeldingBestillingTopicPartition to listOf(
                                    dialogmeldingBestillingRecord,
                                    dialogmeldingBestillingRecordDuplicate,
                                )
                            )
                        )
                        every { mockConsumer.commitSync() } returns Unit

                        runBlocking {
                            pollAndProcessDialogmeldingBestilling(
                                behandlerDialogmeldingService = behandlerDialogmeldingService,
                                kafkaConsumerDialogmeldingBestilling = mockConsumer,
                            )
                        }

                        verify(exactly = 1) { mockConsumer.commitSync() }

                        val pBehandlerDialogmeldingBestilling =
                            database.connection.use {
                                it.getBehandlerDialogmeldingBestilling(
                                    uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid)
                                )
                            }
                        pBehandlerDialogmeldingBestilling shouldNotBeEqualTo null
                        pBehandlerDialogmeldingBestilling!!.uuid shouldBeEqualTo dialogmeldingBestillingUuid
                    }
                }
                describe("Does not persist when behandlerRef not valid") {
                    it("should not persist incoming bestillinger when behandlerRef is invalid") {
                        val behandlerRef = UUID.randomUUID()

                        val dialogmeldingBestillingUuid = UUID.randomUUID()
                        val dialogmeldingBestilling = generateBehandlerDialogmeldingBestillingDTO(
                            uuid = dialogmeldingBestillingUuid,
                            behandlerRef = behandlerRef,
                        )
                        val dialogmeldingBestillingRecord = ConsumerRecord(
                            DIALOGMELDING_BESTILLING_TOPIC,
                            partition,
                            1,
                            dialogmeldingBestilling.dialogmeldingUuid,
                            dialogmeldingBestilling,
                        )
                        val mockConsumer = mockk<KafkaConsumer<String, BehandlerDialogmeldingBestillingDTO>>()
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                behandlerDialogmeldingBestillingTopicPartition to listOf(
                                    dialogmeldingBestillingRecord,
                                )
                            )
                        )
                        every { mockConsumer.commitSync() } returns Unit

                        runBlocking {
                            pollAndProcessDialogmeldingBestilling(
                                behandlerDialogmeldingService = behandlerDialogmeldingService,
                                kafkaConsumerDialogmeldingBestilling = mockConsumer,
                            )
                        }

                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val pBehandlerDialogmeldingBestilling =
                            database.connection.use {
                                it.getBehandlerDialogmeldingBestilling(
                                    uuid = UUID.fromString(dialogmeldingBestilling.dialogmeldingUuid)
                                )
                            }
                        pBehandlerDialogmeldingBestilling shouldBeEqualTo null
                    }
                }
            }
        }
    }
})
