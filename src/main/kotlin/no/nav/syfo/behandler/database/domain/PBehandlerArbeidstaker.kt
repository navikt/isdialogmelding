package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.Personident
import java.time.OffsetDateTime

data class PBehandlerArbeidstaker(
    val id: Int,
    val type: String,
    val arbeidstakerPersonident: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val mottatt: OffsetDateTime,
)

fun PBehandlerArbeidstaker.toArbeidstaker(
    fornavn: String,
    mellomnavn: String?,
    etternavn: String,
) = Arbeidstaker(
    arbeidstakerPersonident = Personident(this.arbeidstakerPersonident),
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    mottatt = mottatt,
)
