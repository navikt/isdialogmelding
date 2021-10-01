package no.nav.syfo.behandler.domain

import no.nav.syfo.behandler.api.BehandlerDialogmeldingDTO

data class Fastlege(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fnr: String,
    val partnerId: Int,
    val herId: Int,
    val helsepersonellregisterId: String?,
    val kontor: Kontor,
) {
    data class Kontor(
        val navn: String?,
        val orgnummer: String?,
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

fun Fastlege.toBehandlerDialogmeldingDTO(): BehandlerDialogmeldingDTO {
    return BehandlerDialogmeldingDTO(
        type = BehandlerType.FASTLEGE.name,
        fornavn = this.fornavn,
        mellomnavn = this.mellomnavn,
        etternavn = this.etternavn,
        fnr = this.fnr,
        partnerId = this.partnerId.toString(),
        herId = this.herId.toString(),
        hprId = this.helsepersonellregisterId,
        orgnummer = this.kontor.orgnummer,
        kontor = this.kontor.navn,
        adresse = this.kontor.postadresse.adresse,
        postnummer = this.kontor.postadresse.postnummer,
        poststed = this.kontor.postadresse.poststed,
        telefon = this.kontor.telefon,
    )
}
