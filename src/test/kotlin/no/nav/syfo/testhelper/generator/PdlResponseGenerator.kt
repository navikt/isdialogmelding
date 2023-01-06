package no.nav.syfo.testhelper.generator

import no.nav.syfo.client.pdl.domain.PdlHentPerson
import no.nav.syfo.client.pdl.domain.PdlPerson
import no.nav.syfo.client.pdl.domain.PdlPersonNavn
import no.nav.syfo.client.pdl.domain.PdlPersonResponse
import no.nav.syfo.testhelper.UserConstants

fun generatePdlPersonResponse() = PdlPersonResponse(
    errors = null,
    data = generatePdlHentPerson(
        generatePdlPersonNavn(),
    )
)

fun generatePdlPersonNavn(): PdlPersonNavn {
    return PdlPersonNavn(
        fornavn = UserConstants.PERSON_FORNAVN,
        mellomnavn = UserConstants.PERSON_MELLOMNAVN,
        etternavn = UserConstants.PERSON_ETTERNAVN,
    )
}

fun generatePdlHentPerson(
    pdlPersonNavn: PdlPersonNavn?,
): PdlHentPerson {
    return PdlHentPerson(
        hentPerson = PdlPerson(
            navn = listOf(
                pdlPersonNavn ?: generatePdlPersonNavn()
            ),
        )
    )
}
