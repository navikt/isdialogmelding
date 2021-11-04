package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerType
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import java.time.LocalDateTime
import java.util.*

data class PBehandlerDialogmelding(
    val id: Int,
    val behandlerRef: UUID,
    val type: String,
    val personident: String?,
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
    val orgnummer: String?,
    val telefon: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun PBehandlerDialogmelding.toBehandler() = Behandler(
    behandlerRef = this.behandlerRef,
    type = BehandlerType.valueOf(this.type),
    personident = this.personident?.let { PersonIdentNumber(it) },
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    partnerId = this.partnerId.toInt(),
    herId = this.herId?.toInt(),
    parentHerId = this.parentHerId?.toInt(),
    hprId = this.hprId?.toInt(),
    kontor = this.kontor,
    adresse = this.adresse,
    postnummer = this.postnummer,
    poststed = this.poststed,
    orgnummer = this.orgnummer?.let { Virksomhetsnummer(it) },
    telefon = this.telefon,
)
