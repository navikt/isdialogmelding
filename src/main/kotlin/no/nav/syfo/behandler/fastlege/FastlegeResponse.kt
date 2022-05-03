package no.nav.syfo.behandler.fastlege

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class FastlegeResponse(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fnr: String?,
    val herId: Int?,
    val foreldreEnhetHerId: Int?,
    val helsepersonellregisterId: Int?,
    val pasient: Pasient,
    val fastlegekontor: Fastlegekontor,
    val pasientforhold: Pasientforhold,
) {
    data class Pasient(
        val fornavn: String?,
        val mellomnavn: String?,
        val etternavn: String?,
        val fnr: String?,
    )

    data class Fastlegekontor(
        val navn: String?,
        val besoeksadresse: Adresse?,
        val postadresse: Adresse?,
        val telefon: String?,
        val epost: String?,
        val orgnummer: String?,
    ) {
        data class Adresse(
            val adresse: String?,
            val postnummer: String?,
            val poststed: String?,
        )
    }

    data class Pasientforhold(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}

fun FastlegeResponse.toBehandler(
    partnerId: PartnerId,
    dialogmeldingEnabled: Boolean = true,
) = Behandler(
    behandlerRef = UUID.randomUUID(),
    kontor = BehandlerKontor(
        partnerId = partnerId,
        herId = this.foreldreEnhetHerId,
        navn = this.fastlegekontor.navn,
        adresse = this.fastlegekontor.postadresse?.adresse,
        postnummer = this.fastlegekontor.postadresse?.postnummer,
        poststed = this.fastlegekontor.postadresse?.poststed,
        orgnummer = this.fastlegekontor.orgnummer?.let { Virksomhetsnummer(it) },
        dialogmeldingEnabled = dialogmeldingEnabled,
        system = null,
        mottatt = OffsetDateTime.now(),
    ),
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    herId = this.herId,
    hprId = this.helsepersonellregisterId,
    personident = this.fnr?.let { PersonIdentNumber(it) },
    telefon = this.fastlegekontor.telefon,
    kategori = BehandlerKategori.LEGE,
    mottatt = OffsetDateTime.now(),
)
