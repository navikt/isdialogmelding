package no.nav.syfo.behandler.domain

import no.nav.syfo.behandler.api.BehandlerDialogmeldingDTO
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import java.util.*

data class Behandler(
    val id: Int,
    val type: BehandlerType,
    val behandlerRef: UUID,
    val personident: PersonIdentNumber?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val partnerId: String,
    val herId: String?,
    val parentHerId: String?,
    val hprId: String?,
    val kontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val orgnummer: Virksomhetsnummer?,
    val telefon: String?,
)

fun Behandler.toBehandlerDialogmeldingDTO() = BehandlerDialogmeldingDTO(
    type = this.type.name,
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    fnr = this.personident?.value,
    partnerId = this.partnerId,
    herId = this.herId.toString(),
    hprId = this.hprId,
    orgnummer = this.orgnummer?.value,
    kontor = this.kontor,
    adresse = this.adresse,
    postnummer = this.postnummer,
    poststed = this.poststed,
    telefon = this.telefon,
)
