package no.nav.syfo.behandler.api

data class BehandlerDTO(
    val type: String?,
    val behandlerRef: String,
    val kategori: String,
    val fnr: String?,
    val hprId: Int?,
    val herId: Int?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val orgnummer: String?,
    val kontor: String?,
    val kontorHerId: Int?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val telefon: String?,
)
