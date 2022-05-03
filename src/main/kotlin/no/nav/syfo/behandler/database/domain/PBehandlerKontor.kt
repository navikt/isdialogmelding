package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.BehandlerKontor
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Virksomhetsnummer
import java.time.OffsetDateTime

data class PBehandlerKontor(
    val id: Int,
    val partnerId: String,
    val herId: String?,
    val navn: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val orgnummer: String?,
    val system: String?,
    val dialogmeldingEnabled: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val mottatt: OffsetDateTime,
)

fun PBehandlerKontor.toBehandlerKontor() = BehandlerKontor(
    partnerId = PartnerId(this.partnerId.toInt()),
    herId = this.herId?.toInt(),
    navn = this.navn,
    adresse = this.adresse,
    postnummer = this.postnummer,
    poststed = this.poststed,
    orgnummer = this.orgnummer?.let { Virksomhetsnummer(it) },
    dialogmeldingEnabled = (this.dialogmeldingEnabled != null),
    system = this.system,
    mottatt = this.mottatt,
)
