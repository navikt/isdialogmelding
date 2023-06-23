package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.DialogmeldingService
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingKodeverk
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingType
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus
import org.slf4j.LoggerFactory

class DialogmeldingSendCronjob(
    val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    val dialogmeldingService: DialogmeldingService,
    val dialogmeldingStatusService: DialogmeldingStatusService,
) : DialogmeldingCronjob {

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 10

    override suspend fun run() {
        dialogmeldingSendJob()
    }

    suspend fun dialogmeldingSendJob(): DialogmeldingCronjobResult {
        val sendingResult = DialogmeldingCronjobResult()

        val bestillinger = dialogmeldingToBehandlerService.getBestillinger()
        bestillinger.forEach { (bestillingId, bestilling) ->
            try {
                dialogmeldingService.sendMelding(bestilling)
                dialogmeldingToBehandlerService.setDialogmeldingBestillingSendt(bestilling.uuid)
                dialogmeldingStatusService.createDialogmeldingStatus(
                    dialogmeldingStatus = DialogmeldingStatus.sendt(bestilling),
                    bestillingId = bestillingId,
                )
                sendingResult.updated++
                COUNT_CRONJOB_DIALOGMELDING_SEND_COUNT.increment()
                when (bestilling.type) {
                    DialogmeldingType.OPPFOLGINGSPLAN -> COUNT_CRONJOB_DIALOGMELDING_OPPFOLGINGSPLAN_SEND_COUNT.increment()
                    DialogmeldingType.DIALOG_NOTAT -> COUNT_CRONJOB_DIALOGMELDING_NOTAT_SEND_COUNT.increment()
                    DialogmeldingType.DIALOG_FORESPORSEL -> {
                        when (bestilling.kodeverk) {
                            DialogmeldingKodeverk.DIALOGMOTE -> COUNT_CRONJOB_DIALOGMELDING_DIALOGMOTE_SEND_COUNT.increment()
                            DialogmeldingKodeverk.FORESPORSEL -> COUNT_CRONJOB_DIALOGMELDING_FORESPORSEL_SEND_COUNT.increment()
                            else -> {}
                        }
                    }
                }
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
