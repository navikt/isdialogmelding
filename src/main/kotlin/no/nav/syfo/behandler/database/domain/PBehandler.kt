package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.domain.PersonIdentNumber
import java.time.LocalDateTime
import java.util.*

data class PBehandler(
    val id: Int,
    val behandlerRef: UUID,
    val kontorId: Int,
    val personident: String?,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val herId: String?,
    val hprId: String?,
    val telefon: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

fun PBehandler.toBehandler(
    kontor: PBehandlerKontor,
) = Behandler(
    behandlerRef = this.behandlerRef,
    kontor = kontor.toBehandlerKontor(),
    personident = this.personident?.let { PersonIdentNumber(it) },
    fornavn = this.fornavn,
    mellomnavn = this.mellomnavn,
    etternavn = this.etternavn,
    herId = this.herId?.toInt(),
    hprId = this.hprId?.toInt(),
    telefon = this.telefon,
)
