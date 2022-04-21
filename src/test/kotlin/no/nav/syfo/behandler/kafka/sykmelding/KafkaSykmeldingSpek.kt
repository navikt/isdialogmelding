package no.nav.syfo.behandler.kafka.sykmelding

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.getBehandlerForArbeidstaker
import no.nav.syfo.behandler.database.getBehandlerKontorForPartnerId
import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*
import java.util.UUID

class KafkaSykmeldingSpek : Spek({

    with(TestApplicationEngine()) {
        start()
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val azureAdClient = AzureAdClient(
            azureAppClientId = externalMockEnvironment.environment.aadAppClient,
            azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
            azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
        )
        val behandlerService = BehandlerService(
            fastlegeClient = FastlegeClient(
                azureAdClient = azureAdClient,
                fastlegeRestClientId = externalMockEnvironment.environment.fastlegeRestClientId,
                fastlegeRestUrl = externalMockEnvironment.environment.fastlegeRestUrl,
            ),
            partnerinfoClient = PartnerinfoClient(
                azureAdClient = azureAdClient,
                syfoPartnerinfoClientId = externalMockEnvironment.environment.syfoPartnerinfoClientId,
                syfoPartnerinfoUrl = externalMockEnvironment.environment.syfoPartnerinfoUrl,
            ),
            database = database,
        )

        val mockConsumer = mockk<KafkaConsumer<String, ReceivedSykmeldingDTO>>()

        beforeEachTest {
            clearMocks(mockConsumer)
            every { mockConsumer.commitSync() } returns Unit
        }

        afterEachTest {
            database.dropData()
        }

        describe(KafkaSykmeldingSpek::class.java.simpleName) {

            describe("Motta sykmelding") {
                val partition = 0
                val sykmeldingTopicPartition = TopicPartition(
                    SYKMELDING_TOPIC,
                    partition,
                )

                describe("Happy path") {
                    it("should persist behandler from incoming sykmelding") {
                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                        )
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmelding.msgId,
                            sykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        val kontorBefore = database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        val behandlerBefore = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        kontorBefore shouldBe null
                        behandlerBefore.size shouldBeEqualTo 0

                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val kontorAfter = database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        kontorAfter!!.partnerId shouldBeEqualTo sykmelding.partnerreferanse
                        kontorAfter.herId shouldBeEqualTo sykmelding.legekontorHerId

                        val behandlerAfter = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfter.size shouldBeEqualTo 1
                        behandlerAfter[0].personident shouldBeEqualTo sykmelding.personNrLege
                        behandlerAfter[0].kategori shouldBeEqualTo BehandlerKategori.LEGE.name
                        behandlerAfter[0].herId shouldBeEqualTo sykmelding.sykmelding.behandler.her
                    }
                    it("should add second behandler from incoming sykmelding") {
                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                        )
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmelding.msgId,
                            sykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val kontorAfter = database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        kontorAfter!!.partnerId shouldBeEqualTo sykmelding.partnerreferanse

                        val behandlerAfter = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfter.size shouldBeEqualTo 1

                        val newSykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                            personNrLege = "02020212346",
                            behandlerFnr = "02020212346",
                            herId = "1234",
                            hprId = "4321",
                        )
                        val newSykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            2,
                            newSykmelding.msgId,
                            newSykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    newSykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 2) { mockConsumer.commitSync() }

                        val behandlerAfterSecondSykmelding = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfterSecondSykmelding.size shouldBeEqualTo 2
                    }
                    it("should not create duplicate when same sykmelder twice") {
                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                        )
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmelding.msgId,
                            sykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val kontorAfter = database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        kontorAfter!!.partnerId shouldBeEqualTo sykmelding.partnerreferanse

                        val behandlerAfter = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfter.size shouldBeEqualTo 1

                        val newSykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                        )
                        val newSykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            newSykmelding.msgId,
                            newSykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    newSykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 2) { mockConsumer.commitSync() }

                        val behandlerAfterSecondSykmelding = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfterSecondSykmelding.size shouldBeEqualTo 1
                    }
                    it("should update system for kontor") {
                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                        )
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmelding.msgId,
                            sykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val kontorAfter = database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        kontorAfter!!.partnerId shouldBeEqualTo sykmelding.partnerreferanse

                        val behandlerAfter = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfter.size shouldBeEqualTo 1

                        val newSykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                            avsenderSystemNavn = "Nytt systemnavn",
                        )
                        val newSykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            newSykmelding.msgId,
                            newSykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    newSykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 2) { mockConsumer.commitSync() }

                        val kontorAfterSecondMessage = database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        kontorAfterSecondMessage!!.partnerId shouldBeEqualTo sykmelding.partnerreferanse
                        kontorAfterSecondMessage.system shouldBeEqualTo newSykmelding.sykmelding.avsenderSystem.navn
                    }
                }
                describe("Invalid sykmelding") {
                    it("should ignore when mismatched behandler fnr") {
                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                            personNrLege = "01010112345",
                            behandlerFnr = "01010112346",
                        )
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmelding.msgId,
                            sykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val kontorAfter =
                            database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        val behandlerAfter =
                            database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        kontorAfter shouldBe null
                        behandlerAfter.size shouldBeEqualTo 0
                    }
                    it("should ignore when invalid behandler kategori") {
                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                            legeHelsepersonellkategori = "XX",
                        )
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmelding.msgId,
                            sykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val kontorAfter =
                            database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        val behandlerAfter =
                            database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        kontorAfter shouldBe null
                        behandlerAfter.size shouldBeEqualTo 0
                    }
                    it("should ignore when missing partnerId") {
                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                            partnerreferanse = "",
                        )
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmelding.msgId,
                            sykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val behandlerAfter =
                            database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfter.size shouldBeEqualTo 0
                    }
                    it("should ignore when mottatt before cutoff") {
                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                            mottattTidspunkt = LocalDateTime.of(LocalDate.of(2021, Month.SEPTEMBER, 1), LocalTime.of(0, 0))
                        )
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmelding.msgId,
                            sykmelding,
                        )
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = behandlerService,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                        val kontorAfter =
                            database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        val behandlerAfter =
                            database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        kontorAfter shouldBe null
                        behandlerAfter.size shouldBeEqualTo 0
                    }
                }
            }
        }
    }
})
