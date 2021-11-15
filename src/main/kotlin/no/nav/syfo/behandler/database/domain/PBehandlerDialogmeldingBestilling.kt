package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import java.util.UUID

data class PBehandlerDialogMeldingBestilling(
    val id: Int,
    val uuid: UUID,
    val behandlerId: Int,
    val arbeidstakerPersonIdent: String,
    val parentUuid: UUID?,
    val conversationUuid: UUID,
    val type: String,
    val kode: Int,
    val tekst: String?,
    val vedlegg: ByteArray? = null,
)

fun PBehandlerDialogMeldingBestilling.toBehandlerDialogmeldingBestilling(
    behandlerRef: UUID
) = BehandlerDialogmeldingBestilling(
    uuid = this.uuid,
    behandlerRef = behandlerRef,
    arbeidstakerPersonIdent = PersonIdentNumber(this.arbeidstakerPersonIdent),
    parentUuid = this.parentUuid,
    conversationUuid = this.conversationUuid,
    type = DialogmeldingType.valueOf(this.type),
    kode = DialogmeldingKode.fromInt(this.kode),
    tekst = this.tekst,
    vedlegg = this.vedlegg,
)
