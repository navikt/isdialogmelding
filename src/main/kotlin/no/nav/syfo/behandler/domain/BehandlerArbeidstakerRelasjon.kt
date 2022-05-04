package no.nav.syfo.behandler.domain

import no.nav.syfo.domain.PersonIdentNumber
import java.time.OffsetDateTime

data class BehandlerArbeidstakerRelasjon(
    val type: BehandlerArbeidstakerRelasjonstype,
    val arbeidstakerPersonident: PersonIdentNumber,
    val fornavn: String = "",
    val mellomnavn: String? = null,
    val etternavn: String = "",
    val mottatt: OffsetDateTime,
)
