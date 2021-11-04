package no.nav.syfo.behandler.domain

import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer

data class Fastlege(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fnr: PersonIdentNumber?,
    val partnerId: Int,
    val herId: Int?,
    val parentHerId: Int?,
    val helsepersonellregisterId: String?,
    val kontor: Kontor,
) {
    data class Kontor(
        val navn: String?,
        val orgnummer: Virksomhetsnummer?,
        val postadresse: Adresse,
        val besoeksadresse: Adresse,
        val telefon: String?,
    )

    data class Adresse(
        val adresse: String?,
        val postnummer: String?,
        val poststed: String?,
    )
}
