package no.nav.syfo.dialogmelding.cronjob

import no.nav.syfo.cronjob.DialogmeldingCronjob
import no.nav.syfo.cronjob.DialogmeldingCronjobResult
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService
import org.slf4j.LoggerFactory

class DialogmeldingStatusCronjob(private val dialogmeldingStatusService: DialogmeldingStatusService) :
    DialogmeldingCronjob {
    override val initialDelayMinutes: Long = 5
    override val intervalDelayMinutes: Long = 10

    override suspend fun run() {
        runJob()
    }

    fun runJob(): DialogmeldingCronjobResult {
        val result = DialogmeldingCronjobResult()

        dialogmeldingStatusService.getUnpublishedDialogmeldingStatus().forEach { dialogmeldingStatus ->
            try {
                dialogmeldingStatusService.publishDialogmeldingStatus(dialogmeldingStatus)
                result.updated++
            } catch (e: Exception) {
                log.error("Caught exception when publishing in DialogmeldingStatusCronjob")
                result.failed++
            }
        }

        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmeldingStatusCronjob::class.java)
    }
}
