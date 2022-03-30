package no.nav.syfo.behandler.kafka.sykmelding

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.util.UUID

class KafkaSykmeldingSpek : Spek({

    with(TestApplicationEngine()) {
        start()
        val database = ExternalMockEnvironment.instance.database

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

                        runBlocking {
                            pollAndProcessSykmelding(
                                kafkaConsumerSykmelding = mockConsumer,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                    }
                }
            }
        }
    }
})
