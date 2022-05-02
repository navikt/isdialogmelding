package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import java.time.OffsetDateTime

data class PBehandlerArbeidstaker(
    val id: Int,
    val type: String,
    val arbeidstakerPersonident: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val kildeTidspunkt: OffsetDateTime,
)

fun PBehandlerArbeidstaker.toBehandlerArbeidstakerRelasjon(
    fornavn: String,
    mellomnavn: String?,
    etternavn: String,
) = BehandlerArbeidstakerRelasjon(
    type = BehandlerArbeidstakerRelasjonstype.valueOf(this.type),
    arbeidstakerPersonident = PersonIdentNumber(this.arbeidstakerPersonident),
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    kildeTidspunkt = kildeTidspunkt,
)
