package no.nav.syfo.oppfolgingsplan.domain

import no.nav.syfo.behandler.api.person.RSOppfolgingsplan
import no.nav.syfo.behandler.domain.Arbeidstaker
import no.nav.syfo.behandler.domain.Behandler

data class RSHodemelding(
    val meldingInfo: RSMeldingInfo?,
    val vedlegg: RSVedlegg?,
)

fun createRSHodemelding(
    behandler: Behandler,
    arbeidstaker: Arbeidstaker,
    oppfolgingsplan: RSOppfolgingsplan,
) = RSHodemelding(
    meldingInfo = tilMeldingInfo(
        mottaker = behandler.tilMottaker(),
        pasient = arbeidstaker.tilPasient(),
    ),
    vedlegg = oppfolgingsplan.tilVedlegg(),
)

private fun tilMeldingInfo(
    mottaker: RSMottaker,
    pasient: RSPasient,
) = RSMeldingInfo(
    mottaker = mottaker,
    pasient = pasient,
)

private fun Arbeidstaker.tilPasient() = RSPasient(
    fnr = this.arbeidstakerPersonident.value,
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
)

private fun RSOppfolgingsplan.tilVedlegg() = RSVedlegg(this.oppfolgingsplanPdf)

private fun Behandler.tilMottaker() = RSMottaker(
    partnerId = this.kontor.partnerId.toString(),
    herId = this.kontor.herId?.toString(),
    orgnummer = this.kontor.orgnummer?.value,
    navn = this.kontor.navn,
    adresse = this.kontor.adresse,
    postnummer = this.kontor.postnummer,
    poststed = this.kontor.poststed,
    behandler = this.tilBehandler(),
)

private fun Behandler.tilBehandler() = RSBehandler(
    fnr = this.personident?.value,
    hprId = this.hprId,
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
)
