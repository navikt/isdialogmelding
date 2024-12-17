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
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.Future

class DialogmeldingStatusCronjobSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
        database = database,
        pdlClient = mockk(),
    )
    val kafkaProducer = mockk<KafkaProducer<String, KafkaDialogmeldingStatusDTO>>()
    val dialogmeldingStatusService = DialogmeldingStatusService(
        database = database,
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        dialogmeldingStatusProducer = DialogmeldingStatusProducer(kafkaProducer)
    )
    val dialogmeldingStatusCronjob = DialogmeldingStatusCronjob(
        dialogmeldingStatusService = dialogmeldingStatusService,
    )

    beforeEachTest {
        database.dropData()
        clearMocks(kafkaProducer)
        coEvery {
            kafkaProducer.send(any())
        } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    describe(DialogmeldingStatusCronjob::class.java.simpleName) {
        it("Publishes unpublished dialogmelding statuses") {
            lagreDialogmeldingStatusBestiltOgSendt(database)

            var result = dialogmeldingStatusCronjob.runJob()
            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 2

            val kafkaRecordSlot1 = slot<ProducerRecord<String, KafkaDialogmeldingStatusDTO>>()
            val kafkaRecordSlot2 = slot<ProducerRecord<String, KafkaDialogmeldingStatusDTO>>()
            verifyOrder {
                kafkaProducer.send(capture(kafkaRecordSlot1))
                kafkaProducer.send(capture(kafkaRecordSlot2))
            }
            val firstPublishedStatus = kafkaRecordSlot1.captured.value()
            val secondPublishedStatus = kafkaRecordSlot2.captured.value()

            firstPublishedStatus.status shouldBeEqualTo DialogmeldingStatusType.BESTILT.name
            secondPublishedStatus.status shouldBeEqualTo DialogmeldingStatusType.SENDT.name
            firstPublishedStatus.bestillingUuid shouldBeEqualTo secondPublishedStatus.bestillingUuid
            firstPublishedStatus.uuid shouldNotBeEqualTo secondPublishedStatus.uuid

            result = dialogmeldingStatusCronjob.runJob()
            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 0

            val pDialogmeldingStatuses = database.getDialogmeldingStatuses()

            pDialogmeldingStatuses.size shouldBeEqualTo 2
            pDialogmeldingStatuses.forEach { pDialogmeldingStatus ->
                pDialogmeldingStatus.publishedAt.shouldNotBeNull()
                pDialogmeldingStatus.createdAt shouldBeLessThan pDialogmeldingStatus.updatedAt
            }
        }
        it("Publishes nothing when no unpublished dialogmelding statuses") {
            val result = dialogmeldingStatusCronjob.runJob()

            result.failed shouldBeEqualTo 0
            result.updated shouldBeEqualTo 0

            verify(exactly = 0) { kafkaProducer.send(any()) }
        }
    }
})
