package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import java.sql.Timestamp
import java.util.UUID

data class PBehandlerDialogMeldingBestilling(
    val id: Int,
    val uuid: UUID,
    val behandlerId: Int,
    val arbeidstakerPersonIdent: String,
    val parentRef: String?,
    val conversationUuid: UUID,
    val type: String,
    val kode: Int,
    val tekst: String?,
    val vedlegg: ByteArray? = null,
    val sendt: Timestamp?,
    val sendtTries: Int,
)

fun PBehandlerDialogMeldingBestilling.toBehandlerDialogmeldingBestilling(
    behandler: Behandler,
) = BehandlerDialogmeldingBestilling(
    uuid = this.uuid,
    behandler = behandler,
    arbeidstakerPersonIdent = PersonIdentNumber(this.arbeidstakerPersonIdent),
    parentRef = this.parentRef,
    conversationUuid = this.conversationUuid,
    type = DialogmeldingType.valueOf(this.type),
    kode = DialogmeldingKode.fromInt(this.kode),
    tekst = this.tekst,
    vedlegg = this.vedlegg,
)
