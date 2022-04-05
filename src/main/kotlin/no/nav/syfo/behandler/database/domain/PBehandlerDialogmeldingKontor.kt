package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.BehandlerKontor
import no.nav.syfo.domain.Virksomhetsnummer
import java.time.LocalDateTime

data class PBehandlerDialogmeldingKontor(
    val id: Int,
    val partnerId: String,
    val herId: Int?,
    val navn: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val orgnummer: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun PBehandlerDialogmeldingKontor.toBehandlerKontor() = BehandlerKontor(
    partnerId = this.partnerId.toInt(),
    herId = this.herId,
    navn = this.navn,
    adresse = this.adresse,
    postnummer = this.postnummer,
    poststed = this.poststed,
    orgnummer = orgnummer?.let { Virksomhetsnummer(it) }
)
