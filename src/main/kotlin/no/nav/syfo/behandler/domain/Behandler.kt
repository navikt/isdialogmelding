package no.nav.syfo.behandler.domain

import no.nav.syfo.behandler.api.BehandlerDTO
import no.nav.syfo.behandler.api.person.PersonBehandlerDTO
import no.nav.syfo.domain.PersonIdentNumber
import java.util.*

data class Behandler(
    val behandlerRef: UUID,
    val personident: PersonIdentNumber?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val herId: Int?,
    val hprId: Int?,
    val telefon: String?,
    val kontor: BehandlerKontor,
    val kategori: BehandlerKategori,
)

fun Behandler.toBehandlerDTO(
    behandlerType: BehandlerArbeidstakerRelasjonType,
) = BehandlerDTO(
    type = behandlerType.name,
    behandlerRef = this.behandlerRef.toString(),
    fnr = this.personident?.value,
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    orgnummer = this.kontor.orgnummer?.value,
    kontor = this.kontor.navn,
    adresse = this.kontor.adresse,
    postnummer = this.kontor.postnummer,
    poststed = this.kontor.poststed,
    telefon = this.telefon,
)

fun Behandler.toPersonBehandlerDTO(
    behandlerType: BehandlerArbeidstakerRelasjonType,
) = PersonBehandlerDTO(
    type = behandlerType.name,
    behandlerRef = this.behandlerRef.toString(),
    kategori = this.kategori.name,
    fnr = this.personident?.value,
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    orgnummer = this.kontor.orgnummer?.value,
    kontor = this.kontor.navn,
    adresse = this.kontor.adresse,
    postnummer = this.kontor.postnummer,
    poststed = this.kontor.poststed,
    telefon = this.telefon,
)

fun Behandler.hasAnId(): Boolean = personident != null || herId != null || hprId != null
