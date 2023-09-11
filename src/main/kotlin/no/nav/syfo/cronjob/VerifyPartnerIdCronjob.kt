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
        verifyPartnerIdJob()
    }

    suspend fun verifyPartnerIdJob() {
        val verifyResult = DialogmeldingCronjobResult()

        val behandlerKontorListe = behandlerService.getKontor().filter {
            it.herId != null && it.dialogmeldingEnabled != null
        }
        behandlerKontorListe.forEach { behandlerKontor ->
            try {
                val partnerIdsForKontor = partnerinfoClient.allPartnerinfo(
                    herId = behandlerKontor.herId!!.toString(),
                    token = "",
                    systemRequest = true,
                    callId = UUID.randomUUID().toString(),
                ).map { it.partnerId }
                if (!partnerIdsForKontor.contains(behandlerKontor.partnerId.toInt())) {
                    log.warn("Kontor med herId ${behandlerKontor.herId} er ikke lengre knyttet til partnerId ${behandlerKontor.partnerId} hos e-mottak")
                    if (behandlerService.existsOtherValidKontorWithSameHerId(behandlerKontor, partnerIdsForKontor)) {
                        behandlerService.disableDialogmeldingerForKontor(behandlerKontor)
                        log.info("Disabled dialogmelding for kontor med partnerId ${behandlerKontor.partnerId}")
                        verifyResult.updated++
                    }
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
    }

    companion object {
        private val log = LoggerFactory.getLogger(VerifyPartnerIdCronjob::class.java)
    }
}
