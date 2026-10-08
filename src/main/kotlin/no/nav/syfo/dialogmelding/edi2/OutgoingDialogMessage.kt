package no.nav.syfo.dialogmelding.edi2

import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingKode
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingKodeverk
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingType
import java.util.Base64

data class OutgoingDialogMessage(
    val version: Int,
    val id: String,
    val patientIdent: String,
    val providerId: String,
    val conversationReference: OutgoingConversationReference?,
    val type: String,
    val message: String?,
    val attachment: String?,
)

data class OutgoingConversationReference(
    val parentMessageId: String,
    val conversationId: String,
)

fun DialogmeldingToBehandlerBestilling.toOutgoingDialogMessage(): OutgoingDialogMessage {
    val outgoingType = toOutgoingType(this)

    val message = if (outgoingType == "FOLLOW_UP_PLAN") {
        "Åpne PDF-vedlegg"
    } else {
        this.getTekstRemoveInvalidCharacters()
    }

    val conversationReference = if (outgoingType == "FOLLOW_UP_PLAN") {
        null
    } else {
        OutgoingConversationReference(
            parentMessageId = this.parentRef ?: this.uuid.toString(),
            conversationId = this.conversationUuid.toString(),
        )
    }

    return OutgoingDialogMessage(
        version = 1,
        id = this.uuid.toString(),
        patientIdent = this.arbeidstakerPersonident.value,
        providerId = this.behandler.behandlerRef.toString(),
        conversationReference = conversationReference,
        type = outgoingType,
        message = message,
        attachment = this.vedlegg?.let { Base64.getEncoder().encodeToString(it) },
    )
}

private fun toOutgoingType(melding: DialogmeldingToBehandlerBestilling): String {
    val kodeverk = melding.kodeverk
        ?: throw IllegalArgumentException("Cannot map dialogmelding without kodeverk for ${melding.uuid}")
    val kode = melding.kode

    return when (melding.type) {
        DialogmeldingType.OPPFOLGINGSPLAN -> "FOLLOW_UP_PLAN"
        DialogmeldingType.DIALOG_FORESPORSEL -> when (Pair(kodeverk, kode)) {
            Pair(DialogmeldingKodeverk.DIALOGMOTE, DialogmeldingKode.KODE1) -> "MEETING_INVITATION_2"
            Pair(DialogmeldingKodeverk.DIALOGMOTE, DialogmeldingKode.KODE2) -> "MEETING_RESCHEDULE_2"
            Pair(DialogmeldingKodeverk.FORESPORSEL, DialogmeldingKode.KODE1) -> "PATIENT_REQUEST"
            Pair(DialogmeldingKodeverk.FORESPORSEL, DialogmeldingKode.KODE2) -> "PATIENT_REQUEST_REMINDER"
            else -> throw IllegalArgumentException("Unsupported mapping for DIALOG_FORESPORSEL with $kodeverk/$kode")
        }

        DialogmeldingType.DIALOG_NOTAT -> when (kode) {
            DialogmeldingKode.KODE2 -> "RETURN_TO_WORK_NOTIFICATION"
            DialogmeldingKode.KODE3 -> "MEDICAL_CERTIFICATE_RETURN"
            DialogmeldingKode.KODE4 -> "MEETING_CANCELLATION"
            DialogmeldingKode.KODE8 -> "NAV_MESSAGE"
            DialogmeldingKode.KODE9 -> "NAV_INFORMATION"
            else -> throw IllegalArgumentException("Unsupported mapping for DIALOG_NOTAT with $kodeverk/$kode")
        }
    }
}
