package no.nav.syfo.behandler.fastlege

import no.nav.syfo.behandler.domain.Fastlege
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import java.time.LocalDate

data class FastlegeResponse(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fnr: String?,
    val herId: Int?,
    val foreldreEnhetHerId: Int?,
    val helsepersonellregisterId: String?,
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

fun FastlegeResponse.Fastlegekontor.toKontor() = Fastlege.Kontor(
    navn = this.navn,
    orgnummer = this.orgnummer?.let { Virksomhetsnummer(it) },
    postadresse = Fastlege.Adresse(
        adresse = this.postadresse?.adresse,
        postnummer = this.postadresse?.postnummer,
        poststed = this.postadresse?.poststed,
    ),
    besoeksadresse = Fastlege.Adresse(
        adresse = this.besoeksadresse?.adresse,
        postnummer = this.besoeksadresse?.postnummer,
        poststed = this.besoeksadresse?.poststed,
    ),
    telefon = this.telefon,
)

fun FastlegeResponse.toFastlege(partnerId: Int) = Fastlege(
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    partnerId = partnerId,
    herId = this.herId,
    parentHerId = this.foreldreEnhetHerId,
    helsepersonellregisterId = this.helsepersonellregisterId,
    fnr = this.fnr?.let { PersonIdentNumber(it) },
    kontor = this.fastlegekontor.toKontor(),
)
