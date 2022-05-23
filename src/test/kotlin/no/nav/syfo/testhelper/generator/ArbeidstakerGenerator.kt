package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.*
import no.nav.syfo.testhelper.UserConstants
import java.time.OffsetDateTime

fun generateArbeidstaker(
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = Arbeidstaker(
    arbeidstakerPersonident = arbeidstakerPersonident,
    fornavn = UserConstants.PERSON_FORNAVN,
    mellomnavn = UserConstants.PERSON_MELLOMNAVN,
    etternavn = UserConstants.PERSON_ETTERNAVN,
    mottatt = OffsetDateTime.now(),
)
