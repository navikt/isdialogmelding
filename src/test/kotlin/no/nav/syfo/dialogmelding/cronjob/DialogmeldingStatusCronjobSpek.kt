package no.nav.syfo.dialogmelding.cronjob

import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.testdata.lagreDialogmeldingStatusBestiltOgSendt
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class DialogmeldingStatusCronjobSpek : Spek({
    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
            database = database,
            pdlClient = mockk(),
        )
        val dialogmeldingStatusService = DialogmeldingStatusService(
            database = database,
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        )
        val dialogmeldingStatusCronjob = DialogmeldingStatusCronjob(
            dialogmeldingStatusService = dialogmeldingStatusService,
        )

        beforeEachTest {
            database.dropData()
        }

        describe(DialogmeldingStatusCronjob::class.java.simpleName) {
            it("Publishes unpublished dialogmelding statuses") {
                lagreDialogmeldingStatusBestiltOgSendt(database)

                var result = dialogmeldingStatusCronjob.runJob()
                result.failed shouldBeEqualTo 0
                result.updated shouldBeEqualTo 2

                result = dialogmeldingStatusCronjob.runJob()
                result.failed shouldBeEqualTo 0
                result.updated shouldBeEqualTo 0

                // TODO: Test Kafka-sending
            }
        }
    }
})
