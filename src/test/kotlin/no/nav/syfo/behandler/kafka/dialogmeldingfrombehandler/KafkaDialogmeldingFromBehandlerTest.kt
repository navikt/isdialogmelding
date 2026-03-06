package no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.getBehandlerByBehandlerRef
import no.nav.syfo.behandler.database.getBehandlerKontor
import no.nav.syfo.behandler.domain.Arbeidstaker
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
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
import java.time.OffsetDateTime
import java.util.*

class KafkaDialogmeldingFromBehandlerTest {
    private val database = ExternalMockEnvironment.instance.database
    private val behandlerService = BehandlerService(
        fastlegeClient = mockk(),
        partnerinfoClient = mockk(),
        database = database,
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Receive dialogmelding from behandler")
    inner class ReceiveDialogmeldingFromBehandler {

        @Nested
        @DisplayName("Happy path")
        inner class HappyPath {
            @Test
            fun `creates kontor if missing`() {
                val dialogmelding = generateDialogmeldingFromBehandlerDTO(UUID.randomUUID())
                val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
                val kontor = database.connection.use { it.getBehandlerKontor(UserConstants.PARTNERID) }
                assertNotNull(kontor)
                assertNotNull(kontor?.dialogmeldingEnabled)
            }

            @Test
            fun `mark kontor as ready to receive dialogmeldinger`() {
                addBehandlerAndKontorToDatabase(behandlerService)
                val dialogmelding = generateDialogmeldingFromBehandlerDTO(UUID.randomUUID())
                val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
                val kontor = database.connection.use { it.getBehandlerKontor(UserConstants.PARTNERID) }
                assertNotNull(kontor?.dialogmeldingEnabled)
            }

            @Test
            fun `do not mark kontor as ready to receive dialogmeldinger if locked`() {
                addBehandlerAndKontorToDatabase(behandlerService)
                database.setDialogmeldingEnabledLocked(UserConstants.PARTNERID.toString())
                val dialogmelding = generateDialogmeldingFromBehandlerDTO(UUID.randomUUID())
                val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
                val kontor = database.connection.use { it.getBehandlerKontor(UserConstants.PARTNERID) }
                assertNull(kontor!!.dialogmeldingEnabled)
            }

            @Test
            fun `update identer for behandler if stored idents are null`() {
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

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
                val behandler = database.getBehandlerByBehandlerRef(behandlerRef)
                assertNotNull(behandler)
                assertEquals(UserConstants.HPRID.toString(), behandler!!.hprId)
                assertEquals(UserConstants.OTHER_HERID.toString(), behandler.herId)
            }

            @Test
            fun `do not update identer when received ident of type XXX`() {
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
                val dialogmelding =
                    generateDialogmeldingFromBehandlerDTO(fellesformatXMLHealthcareProfessionalMedIdenttypeAnnen)
                val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
                val behandler = database.getBehandlerByBehandlerRef(behandlerRef)
                assertNotNull(behandler)
                assertEquals(UserConstants.HPRID.toString(), behandler!!.hprId)
                assertNull(behandler.herId)
            }
        }

        @Nested
        @DisplayName("Unhappy path")
        inner class UnhappyPath {
            @Test
            fun `don't mark kontor as ready to receive dialogmeldinger if no partnerId is found`() {
                addBehandlerAndKontorToDatabase(behandlerService)
                val dialogmelding = generateDialogmeldingFromBehandlerDTOWithInvalidXml(UUID.randomUUID())
                val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
                val kontor = database.connection.use { it.getBehandlerKontor(UserConstants.PARTNERID) }
                assertNotNull(kontor)
                assertNull(kontor!!.dialogmeldingEnabled)
            }

            @Test
            fun `do not update identer for behandler with invalid fnr`() {
                val dialogmelding = generateDialogmeldingFromBehandlerDTO(
                    fellesformatXml = fellesformatXMLHealthcareProfessionalInvalidFNR,
                )
                val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmelding)

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
            }

            @Test
            fun `don't update behandleridenter if we can't find partnerId in xml`() {
                val dialogmeldingWithoutValidPartnerIdWithHealthcareProfessional =
                    generateDialogmeldingFromBehandlerDTO(fellesformatXmlWithIdenterWithoutPartnerId)
                val behandlerRef = addExistingBehandlerToDatabase(database)
                val mockConsumer =
                    mockKafkaConsumerWithDialogmelding(dialogmeldingWithoutValidPartnerIdWithHealthcareProfessional)

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
                val behandler = database.getBehandlerByBehandlerRef(behandlerRef)
                assertNotNull(behandler)
                assertNull(behandler!!.herId)
                assertEquals(UserConstants.HPRID.toString(), behandler.hprId)
            }

            @Test
            fun `don't update behandleridenter if HealthcareProfessional is not in xml`() {
                val dialogmeldingWithoutHealthcareProfessional = generateDialogmeldingFromBehandlerDTO(fellesformatXml)
                val behandlerRef = addExistingBehandlerToDatabase(database)
                val mockConsumer = mockKafkaConsumerWithDialogmelding(dialogmeldingWithoutHealthcareProfessional)

                pollAndProcessDialogmeldingFromBehandler(
                    kafkaConsumerDialogmeldingFromBehandler = mockConsumer,
                    database = database,
                )

                verify(exactly = 1) { mockConsumer.commitSync() }
                val behandler = database.getBehandlerByBehandlerRef(behandlerRef)
                assertNotNull(behandler)
                assertNull(behandler!!.herId)
                assertEquals(UserConstants.HPRID.toString(), behandler.hprId)
            }
        }
    }
}

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
