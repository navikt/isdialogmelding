package no.nav.syfo.dialogmelding.status.kafka

import java.time.OffsetDateTime

data class KafkaDialogmeldingStatusDTO(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val status: String,
    val tekst: String?,
    val bestillingUuid: String,
)
