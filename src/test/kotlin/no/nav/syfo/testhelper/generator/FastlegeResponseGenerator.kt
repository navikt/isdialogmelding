package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.fastlege.FastlegeResponse
import java.time.LocalDate

fun generateFastlegeResponse(foreldreEnhetHerId: Int? = null) = FastlegeResponse(
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    fnr = "12125678911",
    herId = 1337,
    foreldreEnhetHerId = foreldreEnhetHerId,
    helsepersonellregisterId = "1234",
    pasient = FastlegeResponse.Pasient(
        fornavn = null,
        mellomnavn = null,
        etternavn = null,
        fnr = null
    ),
    fastlegekontor = FastlegeResponse.Fastlegekontor(
        navn = "Fastlegens kontor",
        besoeksadresse = null,
        postadresse = null,
        telefon = "",
        epost = "",
        orgnummer = null,
    ),
    pasientforhold = FastlegeResponse.Pasientforhold(
        fom = LocalDate.now().minusDays(10),
        tom = LocalDate.now().plusDays(10),
    )
)
