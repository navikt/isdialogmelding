package no.nav.syfo.dialogmelding.status.domain

import no.nav.syfo.dialogmelding.apprec.domain.Apprec
import no.nav.syfo.dialogmelding.apprec.domain.ApprecStatus
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.util.nowUTC
import java.time.OffsetDateTime
import java.util.*

enum class DialogmeldingStatusType {
    BESTILT, SENDT, OK, AVVIST
}

data class DialogmeldingStatus private constructor(
    val uuid: UUID,
    val bestilling: DialogmeldingToBehandlerBestilling,
    val status: DialogmeldingStatusType,
    val tekst: String? = null,
    val createdAt: OffsetDateTime,
    val publishedAt: OffsetDateTime? = null,
) {
    companion object {
        fun bestilt(bestilling: DialogmeldingToBehandlerBestilling): DialogmeldingStatus = create(
            status = DialogmeldingStatusType.BESTILT,
            bestilling = bestilling,
        )

        fun sendt(bestilling: DialogmeldingToBehandlerBestilling): DialogmeldingStatus = create(
            status = DialogmeldingStatusType.SENDT,
            bestilling = bestilling,
        )

        fun fromApprec(apprec: Apprec): DialogmeldingStatus {
            return when (apprec.statusKode) {
                ApprecStatus.OK -> create(
                    bestilling = apprec.bestilling,
                    status = DialogmeldingStatusType.OK,
                )

                ApprecStatus.AVVIST -> create(
                    bestilling = apprec.bestilling,
                    status = DialogmeldingStatusType.AVVIST,
                    tekst = apprec.feilTekst,
                )
            }
        }

        private fun create(
            status: DialogmeldingStatusType,
            bestilling: DialogmeldingToBehandlerBestilling,
            tekst: String? = null,
        ) = DialogmeldingStatus(
            uuid = UUID.randomUUID(),
            bestilling = bestilling,
            status = status,
            tekst = tekst,
            createdAt = nowUTC(),
        )
    }
}
