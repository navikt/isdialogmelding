package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import org.slf4j.LoggerFactory
import java.util.UUID

class VerifyPartnerIdCronjob(
    val behandlerService: BehandlerService,
    val partnerinfoClient: PartnerinfoClient,
) : DialogmeldingCronjob {

    override val initialDelayMinutes: Long = 20
    override val intervalDelayMinutes: Long = 24 * 60

    override suspend fun run() {
        dialogmeldingVerifyPartnerIdJob()
    }

    suspend fun dialogmeldingVerifyPartnerIdJob(): DialogmeldingCronjobResult {
        val verifyResult = DialogmeldingCronjobResult()

        val behandlerKontorMedHerId = behandlerService.getKontor().filter { it.herId != null }
        behandlerKontorMedHerId.forEach { behandlerKontor ->
            try {
                val partnerIdResponse = partnerinfoClient.allPartnerinfo(
                    herId = behandlerKontor.herId!!.toString(),
                    token = "",
                    systemRequest = true,
                    callId = UUID.randomUUID().toString(),
                )
                if (!partnerIdResponse.map { it.partnerId }.contains(behandlerKontor.partnerId.value)) {
                    log.warn("Kontor med herId ${behandlerKontor.herId} er ikke lengre knyttet til partnerId ${behandlerKontor.partnerId} hos e-mottak")
                }
            } catch (e: Exception) {
                log.error("Exception caught while checking behandlerkontor", e)
                verifyResult.failed++
            }
        }
        log.info(
            "Completed checking behandlerkontor result: {}, {}",
            StructuredArguments.keyValue("failed", verifyResult.failed),
            StructuredArguments.keyValue("updated", verifyResult.updated),
        )
        return verifyResult
    }

    companion object {
        private val log = LoggerFactory.getLogger(VerifyPartnerIdCronjob::class.java)
    }
}
