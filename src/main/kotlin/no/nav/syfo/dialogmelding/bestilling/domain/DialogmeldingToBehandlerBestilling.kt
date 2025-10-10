package no.nav.syfo.dialogmelding.bestilling.domain

import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.domain.Personident
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("no.nav.syfo.dialogmelding")

data class DialogmeldingToBehandlerBestilling(
    val uuid: UUID,
    val behandler: Behandler,
    val arbeidstakerPersonident: Personident,
    val parentRef: String?,
    val conversationUuid: UUID,
    val type: DialogmeldingType,
    val kodeverk: DialogmeldingKodeverk?, // må tillate null her siden persisterte bestillinger kan mangle denne verdien
    val kode: DialogmeldingKode,
    val tekst: String?,
    val vedlegg: ByteArray? = null,
    val kilde: String?,
) {
    fun getTekstRemoveInvalidCharacters(): String? {
        // Allow: TAB, CR, LF, printable ASCII (U+0020..U+007E), and all bytes U+0080..U+00FF (including C1 range) per request.
        // Excludes only DEL (0x7F) and any codepoints > U+00FF. Still normalizes NBSP to space and removes soft hyphen.
        val vasket = tekst
            ?.replace(Regex("[^\\t\\r\\n\\x20-\\x7E\\x80-\\xFF]"), "")
            ?.replace("\u00A0", " ")
            ?.replace("\u00AD", "")
        if (tekst != null && tekst != vasket) {
            log.warn("Fjernet tegn utenfor tillatt byte-område (eller spesialtegn) fra tekst i dialogmeldingbestilling med uuid: $uuid\")")
        }
        return vasket
    }
}

enum class DialogmeldingKode(
    val value: Int
) {
    KODE1(1),
    KODE2(2),
    KODE3(3),
    KODE4(4),
    KODE8(8),
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
