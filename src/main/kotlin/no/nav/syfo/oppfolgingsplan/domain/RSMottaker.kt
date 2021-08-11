package no.nav.syfo.oppfolgingsplan.domain

data class RSMottaker(
    val partnerId: String?,
    val herId: String?,
    val orgnummer: String?,
    val navn: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val behandler: RSBehandler?,
)
