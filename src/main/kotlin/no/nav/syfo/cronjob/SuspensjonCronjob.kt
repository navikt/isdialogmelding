package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.client.btsys.LegeSuspensjonClient
import org.slf4j.LoggerFactory

class SuspensjonCronjob(
    val behandlerService: BehandlerService,
    val legeSuspensjonClient: LegeSuspensjonClient,
) : DialogmeldingCronjob {
    private val runAtHour = 11

    override val initialDelayMinutes: Long = calculateInitialDelay("SuspensjonCronJob", runAtHour)
    override val intervalDelayMinutes: Long = 24 * 60

    override suspend fun run() {
        checkLegeSuspensjonJob()
    }

    suspend fun checkLegeSuspensjonJob() {
        val verifyResult = DialogmeldingCronjobResult()

        behandlerService.getBehandlerPersonidenterForAktiveKontor().forEach { behandlerPersonident ->
            try {
                val suspendert = legeSuspensjonClient.sjekkSuspensjon(behandlerPersonident).suspendert
                if (suspendert) {
                    COUNT_CRONJOB_SUSPENSJON_FOUND_COUNT.increment()
                }
                behandlerService.updateBehandlerSuspensjon(behandlerPersonident, suspendert)
                verifyResult.updated++
            } catch (e: Exception) {
                log.error("Exception caught while checking suspensjon", e)
                verifyResult.failed++
            }
        }
        log.info(
            "Completed checking suspensjon result: {}, {}",
            StructuredArguments.keyValue("failed", verifyResult.failed),
            StructuredArguments.keyValue("updated", verifyResult.updated),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(SuspensjonCronjob::class.java)
    }
}
