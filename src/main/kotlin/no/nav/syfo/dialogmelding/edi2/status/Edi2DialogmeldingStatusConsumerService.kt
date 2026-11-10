package no.nav.syfo.dialogmelding.edi2.status

import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.dialogmelding.apprec.ApprecService
import no.nav.syfo.dialogmelding.apprec.domain.Apprec
import no.nav.syfo.dialogmelding.apprec.domain.ApprecStatus
import no.nav.syfo.dialogmelding.apprec.domain.isOK
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.UUID

private const val STATUS_COMPLETED = "COMPLETED"
private const val STATUS_REJECTED_APPREC = "REJECTED_APPREC"

class Edi2DialogmeldingStatusConsumerService(
    private val apprecService: ApprecService,
    private val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    private val behandlerService: BehandlerService,
) {
    fun handleIncomingStatus(statusMessage: KafkaDialogmeldingStatusFromEdi2DTO) {
        val apprecStatus = when (statusMessage.status) {
            STATUS_COMPLETED -> ApprecStatus.OK
            STATUS_REJECTED_APPREC -> ApprecStatus.AVVIST
            else -> return
        }

        val messageId = runCatching { UUID.fromString(statusMessage.messageId) }.getOrElse {
            log.warn("Ignoring EDI2 status with invalid messageId {}", statusMessage.messageId)
            return
        }
        val syntheticApprecUuid = syntheticApprecUuid(
            messageId = messageId,
            status = statusMessage.status,
        )

        if (apprecService.apprecExists(syntheticApprecUuid)) {
            log.info(
                "Ignoring duplicate EDI2 status {} for dialogmelding {}",
                statusMessage.status,
                messageId,
            )
            return
        }

        val bestillingPair = dialogmeldingToBehandlerService.getBestilling(messageId)
        if (bestillingPair == null) {
            log.info("Received but skipped EDI2 status {} for unknown dialogmelding {}", statusMessage.status, messageId)
            return
        }

        val (bestillingId, bestilling) = bestillingPair
        val apprecError = statusMessage.apprec?.errorList?.firstOrNull()
        val apprec = Apprec(
            uuid = syntheticApprecUuid,
            bestilling = bestilling,
            statusKode = apprecStatus,
            statusTekst = statusMessage.apprec?.status ?: apprecStatus.dn,
            feilKode = apprecError?.code ?: statusMessage.error?.code,
            feilTekst = apprecError?.description ?: statusMessage.error?.details,
        )
        apprecService.createApprec(
            apprec = apprec,
            bestillingId = bestillingId,
        )
        if (apprec.isOK() && !bestilling.behandler.kontor.dialogmeldingEnabled) {
            log.info(
                "Enabling dialogmeldinger for behandlerkontor med partnerId {} siden status {} mottatt",
                bestilling.behandler.kontor.partnerId,
                statusMessage.status,
            )
            behandlerService.enableDialogmeldingerForKontor(
                behandlerKontor = bestilling.behandler.kontor,
            )
        }
    }

    private fun syntheticApprecUuid(messageId: UUID, status: String): UUID {
        return UUID.nameUUIDFromBytes("edi2:$messageId:$status".toByteArray(StandardCharsets.UTF_8))
    }

    companion object {
        private val log = LoggerFactory.getLogger(Edi2DialogmeldingStatusConsumerService::class.java)
    }
}
