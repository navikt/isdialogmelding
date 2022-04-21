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
import java.time.Duration
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
                        val sykmeldingMsgId = UUID.randomUUID()
                        val sykmelding = generateSykmeldingDTO(sykmeldingMsgId)
                        val sykmeldingRecord = ConsumerRecord(
                            SYKMELDING_TOPIC,
                            partition,
                            1,
                            sykmeldingMsgId.toString(),
                            sykmelding,
                        )
                        val mockConsumer = mockk<KafkaConsumer<String, ReceivedSykmeldingDTO>>()
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                sykmeldingTopicPartition to listOf(
                                    sykmeldingRecord,
                                )
                            )
                        )
                        every { mockConsumer.commitSync() } returns Unit
                        val kontorBefore = database.connection.getBehandlerKontorForPartnerId(sykmelding.partnerreferanse!!.toInt())
                        val behandlerBefore = database.getBehandlerForArbeidstaker(PersonIdentNumber(sykmelding.personNrPasient))
                        kontorBefore shouldBe null
                        behandlerBefore.size shouldBeEqualTo 0

                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                                behandlerService = BehandlerService(
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
                }
            }
        }
    }
})
