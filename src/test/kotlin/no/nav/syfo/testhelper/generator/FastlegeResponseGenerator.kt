package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.fastlege.FastlegeResponse
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.testhelper.UserConstants
import java.time.LocalDate

fun generateFastlegeResponse() = FastlegeResponse(
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    fnr = UserConstants.FASTLEGE_FNR.value,
    herId = 1337,
    foreldreEnhetHerId = UserConstants.HERID,
    helsepersonellregisterId = 1234,
    pasient = FastlegeResponse.Pasient(
        fornavn = null,
        mellomnavn = null,
        etternavn = null,
        fnr = null
    ),
    fastlegekontor = FastlegeResponse.Fastlegekontor(
        navn = "Fastlegens kontor",
        besoeksadresse = null,
        postadresse = generateFastlegeResponseAdresse(),
        telefon = "",
        epost = "",
        orgnummer = null,
    ),
    pasientforhold = FastlegeResponse.Pasientforhold(
        fom = LocalDate.now().minusDays(10),
        tom = LocalDate.now().plusDays(10),
    )
)

fun generateFastlegeResponse(personident: PersonIdentNumber) = FastlegeResponse(
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    fnr = personident.value,
    herId = 1337,
    foreldreEnhetHerId = UserConstants.HERID,
    helsepersonellregisterId = 1234,
    pasient = FastlegeResponse.Pasient(
        fornavn = null,
        mellomnavn = null,
        etternavn = null,
        fnr = null
    ),
    fastlegekontor = FastlegeResponse.Fastlegekontor(
        navn = "Fastlegens kontor",
        besoeksadresse = null,
        postadresse = generateFastlegeResponseAdresse(),
        telefon = "",
        epost = "",
        orgnummer = null,
    ),
    pasientforhold = FastlegeResponse.Pasientforhold(
        fom = LocalDate.now().minusDays(10),
        tom = LocalDate.now().plusDays(10),
    )
)

fun generateFastlegeResponse(foreldreEnhetHerId: Int?) = FastlegeResponse(
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    fnr = UserConstants.FASTLEGE_FNR.value,
    herId = 1337,
    foreldreEnhetHerId = foreldreEnhetHerId,
    helsepersonellregisterId = 1234,
    pasient = FastlegeResponse.Pasient(
        fornavn = null,
        mellomnavn = null,
        etternavn = null,
        fnr = null
    ),
    fastlegekontor = FastlegeResponse.Fastlegekontor(
        navn = "Fastlegens kontor",
        besoeksadresse = null,
        postadresse = generateFastlegeResponseAdresse(),
        telefon = "",
        epost = "",
        orgnummer = null,
    ),
    pasientforhold = FastlegeResponse.Pasientforhold(
        fom = LocalDate.now().minusDays(10),
        tom = LocalDate.now().plusDays(10),
    )
)

fun generateFastlegeResponse(personident: PersonIdentNumber?, herId: Int?, hprId: Int?) = FastlegeResponse(
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    fnr = personident?.value,
    herId = herId,
    foreldreEnhetHerId = UserConstants.HERID,
    helsepersonellregisterId = hprId,
    pasient = FastlegeResponse.Pasient(
        fornavn = null,
        mellomnavn = null,
        etternavn = null,
        fnr = null
    ),
    fastlegekontor = FastlegeResponse.Fastlegekontor(
        navn = "Fastlegens kontor",
        besoeksadresse = null,
        postadresse = generateFastlegeResponseAdresse(),
        telefon = "",
        epost = "",
        orgnummer = null,
    ),
    pasientforhold = FastlegeResponse.Pasientforhold(
        fom = LocalDate.now().minusDays(10),
        tom = LocalDate.now().plusDays(10),
    )
)

fun generateFastlegeResponseAdresse() = FastlegeResponse.Fastlegekontor.Adresse(
    adresse = "Storgata 1",
    postnummer = "0651",
    poststed = "Oslo",
)
