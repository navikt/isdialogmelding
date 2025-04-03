package no.nav.syfo.behandler.fastlege

import no.nav.syfo.behandler.domain.BehandlerKontor
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Virksomhetsnummer
import java.time.OffsetDateTime

data class BehandlerKontorFraAdresseregisteretDTO(
    val aktiv: Boolean,
    val herId: Int,
    val navn: String,
    val besoksadresse: Adresse?,
    val postadresse: Adresse?,
    val telefon: String?,
    val epost: String?,
    val orgnummer: String?,
    val behandlere: List<BehandlerFraAdresseregisteretDTO>,
) {
    data class Adresse(
        val adresse: String?,
        val postnummer: String?,
        val poststed: String?,
    )

    data class BehandlerFraAdresseregisteretDTO(
        val aktiv: Boolean,
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val personIdent: String?,
        val herId: Int,
        val hprId: Int?,
        val kategori: String?,
    )
}

fun BehandlerKontorFraAdresseregisteretDTO.toBehandlerKontor(partnerId: String) =
    BehandlerKontor(
        partnerId = PartnerId(partnerId.toInt()),
        herId = this.herId,
        navn = this.navn,
        adresse = this.besoksadresse?.adresse ?: this.postadresse?.adresse,
        postnummer = this.besoksadresse?.postnummer ?: this.postadresse?.postnummer,
        poststed = this.besoksadresse?.poststed ?: this.postadresse?.poststed,
        orgnummer = this.orgnummer?.let { Virksomhetsnummer(it) },
        dialogmeldingEnabled = false,
        dialogmeldingEnabledLocked = false,
        system = null,
        mottatt = OffsetDateTime.now(),
    )
