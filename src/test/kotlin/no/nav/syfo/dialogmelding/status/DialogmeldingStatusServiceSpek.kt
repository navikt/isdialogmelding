package no.nav.syfo.dialogmelding.status

import io.mockk.mockk
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.testdata.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DialogmeldingStatusServiceSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

    val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(database = database, pdlClient = mockk())
    val dialogmeldingStatusService = DialogmeldingStatusService(
        database = database,
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService
    )

    beforeEachTest {
        database.dropData()
    }
    describe("DialogmeldingStatusService") {
        it("gets unpublished statuses from database oldest first") {
            lagreDialogmeldingStatusBestiltOgSendt(database)

            val unpublishedDialogmeldingStatus = dialogmeldingStatusService.getUnpublishedDialogmeldingStatus()

            unpublishedDialogmeldingStatus.size shouldBeEqualTo 2
            unpublishedDialogmeldingStatus.first().status shouldBeEqualTo DialogmeldingStatusType.BESTILT
            unpublishedDialogmeldingStatus.last().status shouldBeEqualTo DialogmeldingStatusType.SENDT
        }
    }
})
