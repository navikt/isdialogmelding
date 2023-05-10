package no.nav.syfo.dialogmelding.status.database

import java.time.OffsetDateTime
import java.util.*

data class PDialogmeldingStatus(
    val id: Int,
    val uuid: UUID,
    val bestillingId: Int,
    val status: String,
    val tekst: String?,
    val updatedAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
    val publishedAt: OffsetDateTime?,
)
