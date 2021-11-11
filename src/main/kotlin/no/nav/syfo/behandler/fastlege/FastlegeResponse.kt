package no.nav.syfo.behandler.fastlege

import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerType
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import java.time.LocalDate
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

fun FastlegeResponse.toBehandler(partnerId: Int) = Behandler(
    type = BehandlerType.FASTLEGE,
    behandlerRef = UUID.randomUUID(),
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    partnerId = partnerId,
    herId = this.herId,
    parentHerId = this.foreldreEnhetHerId,
    hprId = this.helsepersonellregisterId,
    personident = this.fnr?.let { PersonIdentNumber(it) },
    kontor = this.fastlegekontor.navn,
    adresse = this.fastlegekontor.postadresse?.adresse,
    postnummer = this.fastlegekontor.postadresse?.postnummer,
    poststed = this.fastlegekontor.postadresse?.poststed,
    telefon = this.fastlegekontor.telefon,
    orgnummer = this.fastlegekontor.orgnummer?.let { Virksomhetsnummer(it) },
)
