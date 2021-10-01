package no.nav.syfo.behandler.api

data class BehandlerDialogmeldingDTO(
    val type: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fnr: String,
    val partnerId: String,
    val herId: String,
    val hprId: String?,
    val orgnummer: String?,
    val kontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val telefon: String?,
)
