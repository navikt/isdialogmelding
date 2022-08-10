package no.nav.syfo.dialogmelding.apprec.database.domain

import java.time.OffsetDateTime
import java.util.UUID

data class PApprec(
    val id: Int,
    val uuid: UUID,
    val bestillingId: Int,
    val statusKode: String,
    val statusTekst: String,
    val feilKode: String?,
    val feilTekst: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
