package no.nav.syfo.behandler.domain

import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Virksomhetsnummer
import java.time.OffsetDateTime

data class BehandlerKontor(
    val partnerId: PartnerId,
    val herId: Int?,
    val navn: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val orgnummer: Virksomhetsnummer?,
    val dialogmeldingEnabled: Boolean,
    val dialogmeldingEnabledLocked: Boolean,
    val system: String?,
    val mottatt: OffsetDateTime,
)

fun BehandlerKontor.harKomplettAdresse() = !adresse.isNullOrBlank() && !postnummer.isNullOrBlank() && !poststed.isNullOrBlank()
