package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.fastlege.FastlegeResponse
import no.nav.syfo.behandler.fastlege.Pasient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.testhelper.UserConstants
import java.time.LocalDate

fun generateFastlegeResponse(
    foreldreEnhetHerId: Int? = null,
    pasientFnr: PersonIdentNumber = UserConstants.ARBEIDSTAKER_FNR,
    pasientFornavn: String = UserConstants.ARBEIDSTAKER_FORNAVN,
    pasientEtternavn: String = UserConstants.ARBEIDSTAKER_ETTERNAVN,
) = FastlegeResponse(
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    fnr = "12125678911",
    herId = 1337,
    foreldreEnhetHerId = foreldreEnhetHerId,
    helsepersonellregisterId = 1234,
    pasient = Pasient(
        fornavn = pasientFornavn,
        mellomnavn = null,
        etternavn = pasientEtternavn,
        fnr = pasientFnr.value,
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
