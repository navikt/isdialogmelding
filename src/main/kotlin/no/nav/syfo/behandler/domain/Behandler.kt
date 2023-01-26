package no.nav.syfo.behandler.domain

import no.nav.syfo.behandler.api.BehandlerDTO
import no.nav.syfo.behandler.api.person.PersonBehandlerDTO
import no.nav.syfo.domain.Personident
import java.time.OffsetDateTime
import java.util.*

data class Behandler(
    val behandlerRef: UUID,
    val personident: Personident?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val herId: Int?,
    val hprId: Int?,
    val telefon: String?,
    val kontor: BehandlerKontor,
    val kategori: BehandlerKategori,
    val mottatt: OffsetDateTime,
    val invalidated: OffsetDateTime? = null,
)

// TODO: Få med behandlerkategori
fun Behandler.toBehandlerDTO(
    behandlerType: BehandlerArbeidstakerRelasjonstype?,
) = BehandlerDTO(
    type = behandlerType?.name,
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
    behandlerType: BehandlerArbeidstakerRelasjonstype,
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

// Kodeverk 2.16.578.1.12.4.1.1.8116
enum class BehandleridentType {
    FNR,
    HPR,
    HER,
    DNR,
    HNR,
    PNR,
    SEF,
    DKF,
    SSN,
    FPN,
    XXX,
}

fun Behandler.hasAnId(): Boolean = personident != null || herId != null || hprId != null

fun List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>.removeDuplicates() = this.sortFastlegerFirst().distinctBy { it.first.behandlerRef }

fun List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>.sortFastlegerFirst(): List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>> {
    val (fastleger, otherBehandlere) = this.partition { it.second == BehandlerArbeidstakerRelasjonstype.FASTLEGE }
    return fastleger + otherBehandlere
}

fun List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>.toBehandlerDTOList() = this.map { it.first.toBehandlerDTO(it.second) }
fun List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>.toPersonBehandlerDTOList() = this.map { it.first.toPersonBehandlerDTO(it.second) }

fun List<Behandler>.toBehandlerDTOListUtenRelasjonstype() = this.map { it.toBehandlerDTO(null) }
