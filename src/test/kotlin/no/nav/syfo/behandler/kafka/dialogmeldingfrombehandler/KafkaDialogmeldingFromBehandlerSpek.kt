package no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler

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

class KafkaDialogmeldingFromBehandlerSpek : Spek({

    with(TestApplicationEngine()) {
        start()
        val database = ExternalMockEnvironment.instance.database

        afterEachTest {
            database.dropData()
        }

        describe("Read dialogmelding sent from behandler to NAV from Kafka Topic") {

            describe("Receive dialogmelding from behandler") {
                val partition = 0
                val dialogmeldingTopicPartition = TopicPartition(
                    DIALOGMELDING_FROM_BEHANDLER_TOPIC,
                    partition,
                )
                describe("Happy path") {
                    it("should persist behandler from incoming sykmelding") {
                        val dialogmeldingMsgId = UUID.randomUUID()
                        val dialogmelding = generateDialogmeldingFromBehandlerDTO(dialogmeldingMsgId)
                        val dialogmeldingRecord = ConsumerRecord(
                            DIALOGMELDING_FROM_BEHANDLER_TOPIC,
                            partition,
                            1,
                            dialogmeldingMsgId.toString(),
                            dialogmelding,
                        )
                        val mockConsumer = mockk<KafkaConsumer<String, KafkaDialogmeldingFromBehandlerDTO>>()
                        every { mockConsumer.poll(any<Duration>()) } returns ConsumerRecords(
                            mapOf(
                                dialogmeldingTopicPartition to listOf(
                                    dialogmeldingRecord,
                                )
                            )
                        )
                        every { mockConsumer.commitSync() } returns Unit

                        runBlocking {
                            pollAndProcessDialogmeldingFromBehandler(
                                kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            )
                        }
                        verify(exactly = 1) { mockConsumer.commitSync() }
                    }
                }
            }
        }
    }
})
