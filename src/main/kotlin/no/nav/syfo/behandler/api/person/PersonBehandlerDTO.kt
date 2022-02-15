package no.nav.syfo.behandler.api.person

data class PersonBehandlerDTO(
    val type: String,
    val behandlerRef: String,
    val fnr: String?,
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
