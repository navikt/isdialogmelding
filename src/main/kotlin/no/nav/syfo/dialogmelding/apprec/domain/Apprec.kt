package no.nav.syfo.dialogmelding.apprec.domain

import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import java.util.UUID

data class Apprec(
    val uuid: UUID,
    val bestilling: DialogmeldingToBehandlerBestilling,
    val statusKode: ApprecStatus,
    val statusTekst: String,
    val feilKode: String?,
    val feilTekst: String?,
)

fun Apprec.isUkjentMottaker(): Boolean = this.statusKode == ApprecStatus.AVVIST && this.feilKode == "E21"

fun Apprec.isOK(): Boolean = this.statusKode == ApprecStatus.OK
