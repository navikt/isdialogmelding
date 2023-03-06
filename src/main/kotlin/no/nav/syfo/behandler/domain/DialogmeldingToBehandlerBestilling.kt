package no.nav.syfo.behandler.domain

import no.nav.syfo.domain.Personident
import java.util.UUID

data class DialogmeldingToBehandlerBestilling(
    val uuid: UUID,
    val behandler: Behandler,
    val arbeidstakerPersonident: Personident,
    val parentRef: String?,
    val conversationUuid: UUID,
    val type: DialogmeldingType,
    val kodeverk: DialogmeldingKodeverk,
    val kode: DialogmeldingKode,
    val tekst: String?,
    val vedlegg: ByteArray? = null,
)

enum class DialogmeldingKode(
    val value: Int
) {
    KODE1(1),
    KODE2(2),
    KODE4(4),
    KODE9(9);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}

enum class DialogmeldingKodeverk(
    val kodeverkId: String,
) {
    DIALOGMOTE("2.16.578.1.12.4.1.1.8125"),
    HENVENDELSE("2.16.578.1.12.4.1.1.8127"),
    FORESPORSEL("2.16.578.1.12.4.1.1.8129"),
}

enum class DialogmeldingType() {
    DIALOG_FORESPORSEL,
    DIALOG_NOTAT,
    OPPFOLGINGSPLAN,
}
