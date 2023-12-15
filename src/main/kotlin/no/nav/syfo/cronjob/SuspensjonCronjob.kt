package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.client.btsys.LegeSuspensjonClient
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SuspensjonCronjob(
    val behandlerService: BehandlerService,
    val legeSuspensjonClient: LegeSuspensjonClient,
) : DialogmeldingCronjob {
    private val runAtHour = 5

    override val initialDelayMinutes: Long = calculateInitialDelay()
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

    private fun calculateInitialDelay() = calculateInitialDelay(LocalDateTime.now())

    private fun calculateInitialDelay(from: LocalDateTime): Long {
        val nowDate = LocalDate.now()
        val nextTimeToRun = LocalDateTime.of(
            if (from.hour < runAtHour) nowDate else nowDate.plusDays(1),
            LocalTime.of(runAtHour, 0),
        )
        val initialDelay = Duration.between(from, nextTimeToRun).toMinutes()
        log.info("SuspensjonCronJob will run in $initialDelay minutes at $nextTimeToRun")
        return initialDelay
    }

    companion object {
        private val log = LoggerFactory.getLogger(SuspensjonCronjob::class.java)
    }
}
