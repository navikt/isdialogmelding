package no.nav.syfo.behandler.domain

import no.nav.syfo.behandler.api.BehandlerDialogmeldingDTO

data class Fastlege(
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val fnr: String,
    val partnerId: Int,
    val herId: Int,
    val helsepersonellregisterId: String?,
    val kontor: Kontor,
) {
    data class Kontor(
        val orgnummer: String?,
        val adresse: String?,
        val postnummer: String?,
        val poststed: String?,
        val telefon: String,
    )
}

fun Fastlege.toBehandlerDialogmeldingDTO(): BehandlerDialogmeldingDTO {
    return BehandlerDialogmeldingDTO(
        fornavn = this.fornavn,
        mellomnavn = this.mellomnavn,
        etternavn = this.etternavn,
        fnr = this.fnr,
        partnerId = this.partnerId.toString(),
        herId = this.herId.toString(),
        hprId = this.helsepersonellregisterId,
        orgnummer = this.kontor.orgnummer,
        adresse = this.kontor.adresse,
        postnummer = this.kontor.postnummer,
        poststed = this.kontor.poststed,
        telefon = this.kontor.telefon,
    )
}
