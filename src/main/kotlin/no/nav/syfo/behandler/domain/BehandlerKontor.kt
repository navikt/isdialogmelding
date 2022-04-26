package no.nav.syfo.behandler.domain

import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Virksomhetsnummer

data class BehandlerKontor(
    val partnerId: PartnerId,
    val herId: Int?,
    val navn: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val orgnummer: Virksomhetsnummer?,
    val dialogmeldingEnabled: Boolean,
    val system: String?,
)

fun BehandlerKontor.harKomplettAdresse() = !adresse.isNullOrBlank() && !postnummer.isNullOrBlank() && !poststed.isNullOrBlank()
