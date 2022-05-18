package no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.domain.Personident
import java.util.UUID

data class DialogmeldingToBehandlerBestillingDTO(
    val behandlerRef: String,
    val personIdent: String,
    val dialogmeldingUuid: String,
    val dialogmeldingRefParent: String?,
    val dialogmeldingRefConversation: String,
    val dialogmeldingType: String,
    val dialogmeldingKode: Int,
    val dialogmeldingTekst: String?,
    val dialogmeldingVedlegg: ByteArray? = null,
)

fun DialogmeldingToBehandlerBestillingDTO.toDialogmeldingToBehandlerBestilling(
    behandler: Behandler,
) = DialogmeldingToBehandlerBestilling(
    uuid = UUID.fromString(this.dialogmeldingUuid),
    behandler = behandler,
    arbeidstakerPersonident = Personident(this.personIdent),
    parentRef = this.dialogmeldingRefParent,
    conversationUuid = UUID.fromString(this.dialogmeldingRefConversation),
    type = DialogmeldingType.valueOf(dialogmeldingType),
    kode = DialogmeldingKode.fromInt(this.dialogmeldingKode),
    tekst = this.dialogmeldingTekst,
    vedlegg = this.dialogmeldingVedlegg,
)
