package no.nav.syfo.behandler.domain

enum class BehandlerKategori(
    val kategoriKode: String,
) {
    FYSIOTERAPEUT("FT"),
    KIROPRAKTOR("KI"),
    LEGE("LE"),
    MANUELLTERAPEUT("MT"),
    TANNLEGE("TL");

    companion object {
        fun fromKategoriKode(kategori: String?): BehandlerKategori? =
            values().firstOrNull { it.kategoriKode == kategori }
    }
}
