package no.nav.syfo.behandler.domain

import no.nav.syfo.domain.Personident
import java.time.OffsetDateTime

data class Arbeidstaker(
    val arbeidstakerPersonident: Personident,
    val fornavn: String = "",
    val mellomnavn: String? = null,
    val etternavn: String = "",
    val mottatt: OffsetDateTime,
)
