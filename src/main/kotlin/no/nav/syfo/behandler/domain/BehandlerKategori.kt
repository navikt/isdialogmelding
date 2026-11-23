package no.nav.syfo.behandler.domain

enum class BehandlerKategori(
    val kategoriKode: String?,
) {
    FYSIOTERAPEUT("FT"),
    KIROPRAKTOR("KI"),
    LEGE("LE"),
    MANUELLTERAPEUT("MT"),
    TANNLEGE("TL"),
    PSYKOLOG("PS"),

    // Tjeneste på et behandlerkontor (adresserbar med HER-id, uten HPR-nummer/helsepersonellkategori)
    TJENESTE(null);

    companion object {
        fun fromKategoriKode(kategori: String?): BehandlerKategori? =
            kategori?.let { kode -> entries.firstOrNull { it.kategoriKode == kode } }
    }
}
