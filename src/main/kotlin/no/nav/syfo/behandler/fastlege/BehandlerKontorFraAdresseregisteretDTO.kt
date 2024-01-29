package no.nav.syfo.behandler.fastlege

data class BehandlerKontorFraAdresseregisteretDTO(
    val aktiv: Boolean,
    val herId: Int,
    val navn: String,
    val besoksadresse: Adresse?,
    val postadresse: Adresse?,
    val telefon: String?,
    val epost: String?,
    val orgnummer: String?,
    val behandlere: List<BehandlerFraAdresseregisteretDTO>,
) {
    data class Adresse(
        val adresse: String?,
        val postnummer: String?,
        val poststed: String?,
    )

    data class BehandlerFraAdresseregisteretDTO(
        val aktiv: Boolean,
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val personIdent: String?,
        val herId: Int,
        val hprId: Int?,
        val kategori: String?,
    )
}
