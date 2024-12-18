package no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.getBehandlerByBehandlerRef
import no.nav.syfo.behandler.database.getBehandlerKontor
import no.nav.syfo.behandler.domain.Arbeidstaker
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*

class KafkaDialogmeldingFromBehandlerSpek : Spek({
    val database = ExternalMockEnvironment.instance.database
    val behandlerService = BehandlerService(
        fastlegeClient = mockk(),
        partnerinfoClient = mockk(),
        database = database,
    )

    afterEachTest {
        database.dropData()
    }

    describe("Read dialogmelding sent from behandler to NAV from Kafka Topic") {

        describe("Receive dialogmelding from behandler") {
            describe("Happy path") {
                it("mark kontor as ready to receive dialogmeldinger") {
                    addBehandlerAndKontorToDatabase(behandlerService)
                    val dialogmelding = generateDialogmeldingFromBehandlerDTO(UUID.randomUUID())
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                    val kontor = database.connection.getBehandlerKontor(UserConstants.PARTNERID)
                    kontor?.dialogmeldingEnabled shouldNotBe null
                }
                it("do not mark kontor as ready to receive dialogmeldinger if locked") {
                    addBehandlerAndKontorToDatabase(behandlerService)
                    database.setDialogmeldingEnabledLocked(UserConstants.PARTNERID.toString())
                    val dialogmelding = generateDialogmeldingFromBehandlerDTO(UUID.randomUUID())
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                    val kontor = database.connection.getBehandlerKontor(UserConstants.PARTNERID)
                    kontor!!.dialogmeldingEnabled shouldBe null
                }
                it("update identer for behandler if stored idents are null") {
                    val behandlerRef = database.createBehandlerForArbeidstaker(
                        behandler = generateBehandler(
                            behandlerRef = UUID.randomUUID(),
                            partnerId = UserConstants.PARTNERID,
                            herId = null,
                            hprId = UserConstants.HPRID,
                        ),
                        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                        relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
                    )
                    val dialogmelding = generateDialogmeldingFromBehandlerDTO(fellesformatXMLHealthcareProfessional)
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                    val behandler = database.getBehandlerByBehandlerRef(behandlerRef)
                    behandler shouldNotBe null
                    behandler!!.hprId shouldBeEqualTo UserConstants.HPRID.toString()
                    behandler.herId shouldBeEqualTo UserConstants.OTHER_HERID.toString()
                }
                it("do not update identer when received ident of type XXX") {
                    val behandlerRef = database.createBehandlerForArbeidstaker(
                        behandler = generateBehandler(
                            behandlerRef = UUID.randomUUID(),
                            partnerId = UserConstants.PARTNERID,
                            herId = null,
                            hprId = UserConstants.HPRID,
                        ),
                        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                        relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
                    )
                    val dialogmelding = generateDialogmeldingFromBehandlerDTO(fellesformatXMLHealthcareProfessionalMedIdenttypeAnnen)
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                    val behandler = database.getBehandlerByBehandlerRef(behandlerRef)
                    behandler shouldNotBe null
                    behandler!!.hprId shouldBeEqualTo UserConstants.HPRID.toString()
                    behandler.herId shouldBeEqualTo null
                }
            }

            describe("Unhappy path") {
                it("don't mark kontor as ready to receive dialogmeldinger if kontor isn't found in database") {
                    val dialogmelding = generateDialogmeldingFromBehandlerDTO(UUID.randomUUID())
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                    val kontor = database.connection.getBehandlerKontor(UserConstants.PARTNERID)
                    kontor `should be` null
                }

                it("don't mark kontor as ready to receive dialogmeldinger if no partnerId is found") {
                    addBehandlerAndKontorToDatabase(behandlerService)
                    val dialogmelding = generateDialogmeldingFromBehandlerDTOWithInvalidXml(UUID.randomUUID())
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                    val kontor = database.connection.getBehandlerKontor(UserConstants.PARTNERID)
                    kontor shouldNotBe null
                    kontor!!.dialogmeldingEnabled `should be` null
                }

                it("do not update identer for behandler with invalid fnr") {
                    val dialogmelding = generateDialogmeldingFromBehandlerDTO(
                        fellesformatXml = fellesformatXMLHealthcareProfessionalInvalidFNR,
                    )
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                }

                it("don't update behandleridenter if we can't find partnerId in xml") {
                    val dialogmeldingWithoutValidPartnerIdWithHealthcareProfessional = generateDialogmeldingFromBehandlerDTO(fellesformatXmlWithIdenterWithoutPartnerId)
                    val behandlerRef = addExistingBehandlerToDatabase(database)
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmeldingWithoutValidPartnerIdWithHealthcareProfessional)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                    val behandler = database.getBehandlerByBehandlerRef(behandlerRef)
                    behandler shouldNotBe null
                    behandler!!.herId shouldBe null
                    behandler.hprId shouldBeEqualTo UserConstants.HPRID.toString()
                }

                it("don't update behandleridenter if HealthcareProfessional is not in xml") {
                    val dialogmeldingWithoutHealthcareProfessional = generateDialogmeldingFromBehandlerDTO(fellesformatXml)
                    val behandlerRef = addExistingBehandlerToDatabase(database)
                    val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmeldingWithoutHealthcareProfessional)

                    runBlocking {
                        pollAndProcessDialogmeldingFromBehandler(
                            kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                            database = database,
                        )
                    }

                    verify(exactly = 1) { mockConsumer.commitSync() }
                    val behandler = database.getBehandlerByBehandlerRef(behandlerRef)
                    behandler shouldNotBe null
                    behandler!!.herId shouldBe null
                    behandler.hprId shouldBeEqualTo UserConstants.HPRID.toString()
                }
            }
        }
    }
})

fun addExistingBehandlerToDatabase(database: TestDatabase): UUID {
    val existingBehandler = generateBehandler(
        behandlerRef = UUID.randomUUID(),
        partnerId = UserConstants.PARTNERID,
        herId = null,
        hprId = UserConstants.HPRID,
    )
    return database.createBehandlerForArbeidstaker(
        behandler = existingBehandler,
        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
        relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
    )
}

fun addBehandlerAndKontorToDatabase(behandlerService: BehandlerService) {
    val behandler = generateFastlegeResponse().toBehandler(
        partnerId = UserConstants.PARTNERID,
        dialogmeldingEnabled = false,
    )

    val arbeidstaker = Arbeidstaker(
        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
        mottatt = OffsetDateTime.now(),
    )

    behandlerService.createOrGetBehandler(
        behandler,
        arbeidstaker,
        BehandlerArbeidstakerRelasjonstype.FASTLEGE,
    )
}

fun mockKafkaConsumerWithDialogmelding(dialogmelding: KafkaDialogmeldingFromBehandlerDTO): KafkaConsumer<String, KafkaDialogmeldingFromBehandlerDTO> {
    val partition = 0
    val dialogmeldingTopicPartition = TopicPartition(
        DIALOGMELDING_FROM_BEHANDLER_TOPIC,
        partition,
    )

    val dialogmeldingRecord = ConsumerRecord(
        DIALOGMELDING_FROM_BEHANDLER_TOPIC,
        partition,
        1,
        dialogmelding.msgId,
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

    return mockConsumer
}
