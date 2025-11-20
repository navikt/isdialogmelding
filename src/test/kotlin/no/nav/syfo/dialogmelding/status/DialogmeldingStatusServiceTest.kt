package no.nav.syfo.dialogmelding.status

import io.mockk.mockk
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.dialogmelding.status.kafka.DialogmeldingStatusProducer
import no.nav.syfo.dialogmelding.status.kafka.KafkaDialogmeldingStatusDTO
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.testdata.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DialogmeldingStatusServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(database = database, pdlClient = mockk())
    private val dialogmeldingStatusService = DialogmeldingStatusService(
        database = database,
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        dialogmeldingStatusProducer = DialogmeldingStatusProducer(
            kafkaProducer = mockk<KafkaProducer<String, KafkaDialogmeldingStatusDTO>>()
        )
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Test
    fun `gets unpublished statuses from database oldest first`() {
        lagreDialogmeldingStatusBestiltOgSendt(database)

        val unpublishedDialogmeldingStatus = dialogmeldingStatusService.getUnpublishedDialogmeldingStatus()

        assertEquals(2, unpublishedDialogmeldingStatus.size)
        assertEquals(DialogmeldingStatusType.BESTILT, unpublishedDialogmeldingStatus.first().status)
        assertEquals(DialogmeldingStatusType.SENDT, unpublishedDialogmeldingStatus.last().status)
    }
}
