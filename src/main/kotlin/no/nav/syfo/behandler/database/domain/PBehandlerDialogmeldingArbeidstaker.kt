package no.nav.syfo.behandler.database.domain

import java.time.LocalDateTime

data class PBehandlerDialogmeldingArbeidstaker(
    val id: Int,
    val arbeidstakerPersonident: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val createdAt: LocalDateTime,
)
