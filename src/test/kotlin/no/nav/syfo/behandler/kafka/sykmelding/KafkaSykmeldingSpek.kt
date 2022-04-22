package no.nav.syfo.behandler.kafka.sykmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.api.*
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import no.nav.syfo.util.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*
import java.util.UUID

class KafkaSykmeldingSpek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    with(TestApplicationEngine()) {
        start()
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        application.testApiModule(externalMockEnvironment = externalMockEnvironment)
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

                        val behandlerRelasjonAfter = database.getBehandlerArbeidstakerRelasjon(
                            personIdentNumber = PersonIdentNumber(sykmelding.personNrPasient),
                            behandlerRef = behandlerAfter[0].behandlerRef,
                        )
                        behandlerRelasjonAfter.size shouldBeEqualTo 1
                        behandlerRelasjonAfter[0].type shouldBeEqualTo BehandlerArbeidstakerRelasjonType.SYKMELDER.name
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
                    it("sykmelding from existing fastlege should leave behandler unchanged") {
                        val behandler =
                            behandlerService.createOrGetBehandler(
                                generateFastlegeResponse().toBehandler(UserConstants.PARTNERID),
                                BehandlerArbeidstakerRelasjon(
                                    type = BehandlerArbeidstakerRelasjonType.FASTLEGE,
                                    arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                                )
                            )

                        val pBehandlerList = database.getBehandlerForArbeidstaker(
                            UserConstants.ARBEIDSTAKER_FNR,
                        )
                        pBehandlerList.size shouldBeEqualTo 1

                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                            personNrPasient = UserConstants.ARBEIDSTAKER_FNR.value,
                            personNrLege = behandler.personident!!.value,
                            behandlerFnr = behandler.personident!!.value,
                            herId = behandler.herId.toString(),
                            hprId = behandler.hprId.toString(),
                            partnerreferanse = UserConstants.PARTNERID.toString(),
                            kontorHerId = UserConstants.HERID.toString(),
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
                        kontorAfter.herId shouldBeEqualTo sykmelding.legekontorHerId

                        val pBehandlerListAfter = database.getBehandlerForArbeidstaker(
                            UserConstants.ARBEIDSTAKER_FNR,
                        )
                        pBehandlerListAfter.size shouldBeEqualTo 1
                        pBehandlerListAfter[0].personident shouldBeEqualTo sykmelding.personNrLege
                        pBehandlerListAfter[0].kategori shouldBeEqualTo BehandlerKategori.LEGE.name
                        pBehandlerListAfter[0].herId shouldBeEqualTo sykmelding.sykmelding.behandler.her

                        val behandlerRelasjonAfter = database.getBehandlerArbeidstakerRelasjon(
                            personIdentNumber = UserConstants.ARBEIDSTAKER_FNR,
                            behandlerRef = behandler.behandlerRef,
                        )
                        behandlerRelasjonAfter.size shouldBeEqualTo 1
                        behandlerRelasjonAfter[0].type shouldBeEqualTo BehandlerArbeidstakerRelasjonType.FASTLEGE.name
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
                    it("behandler from sykmelding should not shadow existing fastlege") {
                        val url = "$behandlerPath$behandlerPersonident"
                        val validToken = generateJWT(
                            externalMockEnvironment.environment.aadAppClient,
                            externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                            UserConstants.VEILEDER_IDENT,
                        )
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val behandlerList =
                                objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 1
                        }

                        val sykmelding = generateSykmeldingDTO(
                            uuid = UUID.randomUUID(),
                            personNrPasient = UserConstants.ARBEIDSTAKER_FNR.value,
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

                        val behandlerAfter = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfter.size shouldBeEqualTo 2
                        val fastlegeBehandlerRef = behandlerAfter[1].behandlerRef

                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR.value)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val behandlerList = objectMapper.readValue<List<BehandlerDTO>>(response.content!!)
                            behandlerList.size shouldBeEqualTo 1
                            behandlerList[0].behandlerRef shouldBeEqualTo fastlegeBehandlerRef.toString()
                        }

                        val behandlerAfterAnotherGet = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        behandlerAfterAnotherGet.size shouldBeEqualTo 2
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
