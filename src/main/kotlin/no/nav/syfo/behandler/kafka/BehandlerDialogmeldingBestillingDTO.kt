package no.nav.syfo.behandler.kafka

import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import java.util.UUID

data class BehandlerDialogmeldingBestillingDTO(
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

fun BehandlerDialogmeldingBestillingDTO.toBehandlerDialogmeldingBestilling(
    behandler: Behandler,
) = BehandlerDialogmeldingBestilling(
    uuid = UUID.fromString(this.dialogmeldingUuid),
    behandler = behandler,
    arbeidstakerPersonIdent = PersonIdentNumber(this.personIdent),
    parentUuid = this.dialogmeldingRefParent?.let { UUID.fromString(it) },
    conversationUuid = UUID.fromString(this.dialogmeldingRefConversation),
    type = DialogmeldingType.valueOf(dialogmeldingType),
    kode = DialogmeldingKode.fromInt(this.dialogmeldingKode),
    tekst = this.dialogmeldingTekst,
    vedlegg = this.dialogmeldingVedlegg,
)
