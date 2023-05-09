package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.DialogmeldingService
import org.slf4j.LoggerFactory

class DialogmeldingSendCronjob(
    val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    val dialogmeldingService: DialogmeldingService,
) : DialogmeldingCronjob {

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 10

    override suspend fun run() {
        dialogmeldingSendJob()
    }

    suspend fun dialogmeldingSendJob(): DialogmeldingCronjobResult {
        val sendingResult = DialogmeldingCronjobResult()

        val bestillinger = dialogmeldingToBehandlerService.getBestillinger()
        bestillinger.forEach { bestilling ->
            try {
                dialogmeldingService.sendMelding(bestilling)
                dialogmeldingToBehandlerService.setDialogmeldingBestillingSendt(bestilling.uuid)
                sendingResult.updated++
                COUNT_CRONJOB_DIALOGMELDING_SEND_COUNT.increment()
            } catch (e: Exception) {
                log.error("Exception caught while attempting sending of dialogmelding", e)
                dialogmeldingToBehandlerService.incrementDialogmeldingBestillingSendtTries(bestilling.uuid)
                sendingResult.failed++
                COUNT_CRONJOB_DIALOGMELDING_FAIL_COUNT.increment()
            }
        }
        log.info(
            "Completed dialogmelding-sending with result: {}, {}",
            StructuredArguments.keyValue("failed", sendingResult.failed),
            StructuredArguments.keyValue("updated", sendingResult.updated),
        )
        return sendingResult
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmeldingSendCronjob::class.java)
    }
}
