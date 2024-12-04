package no.nav.syfo.behandler.api

data class BehandlerDTO(
    val type: String?,
    val behandlerRef: String,
    val kategori: String,
    val fnr: String?,
    val hprId: Int?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val orgnummer: String?,
    val kontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val telefon: String?,
)
