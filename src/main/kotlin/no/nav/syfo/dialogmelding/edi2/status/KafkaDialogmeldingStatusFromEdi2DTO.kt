package no.nav.syfo.dialogmelding.edi2.status

data class KafkaDialogmeldingStatusFromEdi2DTO(
    val messageId: String,
    val timestamp: String?,
    val status: String,
    val apprec: KafkaDialogmeldingStatusFromEdi2ApprecDTO?,
    val error: KafkaDialogmeldingStatusFromEdi2ErrorDTO?,
)

data class KafkaDialogmeldingStatusFromEdi2ApprecDTO(
    val receiverHerId: Int?,
    val status: String?,
    val errorList: List<KafkaDialogmeldingStatusFromEdi2ApprecErrorDTO> = emptyList(),
)

data class KafkaDialogmeldingStatusFromEdi2ApprecErrorDTO(
    val code: String?,
    val description: String?,
    val oid: String?,
)

data class KafkaDialogmeldingStatusFromEdi2ErrorDTO(
    val code: String,
    val details: String,
)
