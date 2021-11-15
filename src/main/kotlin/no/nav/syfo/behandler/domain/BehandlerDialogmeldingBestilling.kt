package no.nav.syfo.behandler.domain

import no.nav.syfo.domain.PersonIdentNumber
import java.util.UUID

data class BehandlerDialogmeldingBestilling(
    val uuid: UUID,
    val behandlerRef: UUID,
    val arbeidstakerPersonIdent: PersonIdentNumber,
    val parentUuid: UUID?,
    val conversationUuid: UUID,
    val type: DialogmeldingType,
    val kode: DialogmeldingKode,
    val tekst: String?,
    val vedlegg: ByteArray? = null,
)

enum class DialogmeldingKode(
    val value: Int
) {
    INNKALLING(1),
    TIDSTED(2),
    AVLYST(4),
    REFERAT(9);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}

enum class DialogmeldingType() {
    DIALOG_FORESPORSEL,
    DIALOG_NOTAT,
}
