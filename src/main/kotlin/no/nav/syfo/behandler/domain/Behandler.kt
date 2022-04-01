package no.nav.syfo.behandler.domain

import no.nav.syfo.behandler.api.BehandlerDialogmeldingDTO
import no.nav.syfo.behandler.api.person.PersonBehandlerDTO
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import java.util.*

data class Behandler(
    val behandlerRef: UUID,
    val personident: PersonIdentNumber?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val partnerId: Int,
    val herId: Int?,
    val parentHerId: Int?,
    val hprId: Int?,
    val kontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val orgnummer: Virksomhetsnummer?,
    val telefon: String?,
)

fun Behandler.toBehandlerDialogmeldingDTO(
    behandlerType: BehandlerType,
) = BehandlerDialogmeldingDTO(
    type = behandlerType.name,
    behandlerRef = this.behandlerRef.toString(),
    fnr = this.personident?.value,
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    orgnummer = this.orgnummer?.value,
    kontor = this.kontor,
    adresse = this.adresse,
    postnummer = this.postnummer,
    poststed = this.poststed,
    telefon = this.telefon,
)

fun Behandler.toPersonBehandlerDTO(
    behandlerType: BehandlerType,
) = PersonBehandlerDTO(
    type = behandlerType.name,
    behandlerRef = this.behandlerRef.toString(),
    fnr = this.personident?.value,
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    orgnummer = this.orgnummer?.value,
    kontor = this.kontor,
    adresse = this.adresse,
    postnummer = this.postnummer,
    poststed = this.poststed,
    telefon = this.telefon,
)

fun Behandler.hasAnId(): Boolean = personident != null || herId != null || hprId != null
