package no.nav.syfo.dialogmelding.cronjob

import io.mockk.*
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.dialogmelding.status.kafka.DialogmeldingStatusProducer
import no.nav.syfo.dialogmelding.status.kafka.KafkaDialogmeldingStatusDTO
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.getDialogmeldingStatuses
import no.nav.syfo.testhelper.testdata.lagreDialogmeldingStatusBestiltOgSendt
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class DialogmeldingStatusCronjobTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
        database = database,
        pdlClient = mockk(),
    )
    private val kafkaProducer = mockk<KafkaProducer<String, KafkaDialogmeldingStatusDTO>>()
    private val dialogmeldingStatusService = DialogmeldingStatusService(
        database = database,
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        dialogmeldingStatusProducer = DialogmeldingStatusProducer(kafkaProducer)
    )
    private val dialogmeldingStatusCronjob = DialogmeldingStatusCronjob(
        dialogmeldingStatusService = dialogmeldingStatusService,
    )

    @BeforeEach
    fun beforeEach() {
        database.dropData()
        clearMocks(kafkaProducer)
        coEvery {
            kafkaProducer.send(any())
        } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    @Test
    fun `Publishes unpublished dialogmelding statuses`() {
        lagreDialogmeldingStatusBestiltOgSendt(database)

        var result = dialogmeldingStatusCronjob.runJob()
        assertEquals(0, result.failed)
        assertEquals(2, result.updated)

        val kafkaRecordSlot1 = slot<ProducerRecord<String, KafkaDialogmeldingStatusDTO>>()
        val kafkaRecordSlot2 = slot<ProducerRecord<String, KafkaDialogmeldingStatusDTO>>()
        verifyOrder {
            kafkaProducer.send(capture(kafkaRecordSlot1))
            kafkaProducer.send(capture(kafkaRecordSlot2))
        }
        val firstPublishedStatus = kafkaRecordSlot1.captured.value()
        val secondPublishedStatus = kafkaRecordSlot2.captured.value()

        assertEquals(DialogmeldingStatusType.BESTILT.name, firstPublishedStatus.status)
        assertEquals(DialogmeldingStatusType.SENDT.name, secondPublishedStatus.status)
        assertEquals(secondPublishedStatus.bestillingUuid, firstPublishedStatus.bestillingUuid)
        assertNotEquals(secondPublishedStatus.uuid, firstPublishedStatus.uuid)

        result = dialogmeldingStatusCronjob.runJob()
        assertEquals(0, result.failed)
        assertEquals(0, result.updated)

        val pDialogmeldingStatuses = database.getDialogmeldingStatuses()

        assertEquals(2, pDialogmeldingStatuses.size)
        pDialogmeldingStatuses.forEach { pDialogmeldingStatus ->
            assertNotNull(pDialogmeldingStatus.publishedAt)
            assertTrue(pDialogmeldingStatus.createdAt < pDialogmeldingStatus.updatedAt)
        }
    }

    @Test
    fun `Publishes nothing when no unpublished dialogmelding statuses`() {
        val result = dialogmeldingStatusCronjob.runJob()

        assertEquals(0, result.failed)
        assertEquals(0, result.updated)

        verify(exactly = 0) { kafkaProducer.send(any()) }
    }
}
