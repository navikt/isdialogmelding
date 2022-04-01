package no.nav.syfo.behandler.database.domain

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import java.time.LocalDateTime

data class PBehandlerDialogmeldingArbeidstaker(
    val id: Int,
    val type: String,
    val arbeidstakerPersonident: String,
    val createdAt: LocalDateTime,
)

fun PBehandlerDialogmeldingArbeidstaker.toBehandlerDialogmeldingArbeidstaker(
    fornavn: String,
    mellomnavn: String?,
    etternavn: String,
) = BehandlerDialogmeldingArbeidstaker(
    type = BehandlerType.valueOf(this.type),
    arbeidstakerPersonident = PersonIdentNumber(this.arbeidstakerPersonident),
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
)
