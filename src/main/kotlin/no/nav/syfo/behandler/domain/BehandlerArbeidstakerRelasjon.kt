package no.nav.syfo.behandler.domain

import no.nav.syfo.domain.PersonIdentNumber

data class BehandlerArbeidstakerRelasjon(
    val type: BehandlerType,
    val arbeidstakerPersonident: PersonIdentNumber,
    val fornavn: String = "",
    val mellomnavn: String? = null,
    val etternavn: String = "",
)
