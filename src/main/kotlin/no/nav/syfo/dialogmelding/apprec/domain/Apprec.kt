package no.nav.syfo.dialogmelding.apprec.domain

import no.nav.syfo.behandler.domain.DialogmeldingToBehandlerBestilling
import java.util.UUID

data class Apprec(
    val uuid: UUID,
    val bestilling: DialogmeldingToBehandlerBestilling,
    val statusKode: ApprecStatus,
    val statusTekst: String,
    val feilKode: String?,
    val feilTekst: String?,
)
