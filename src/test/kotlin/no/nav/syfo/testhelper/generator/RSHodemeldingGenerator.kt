package no.nav.syfo.testhelper.generator

import no.nav.syfo.oppfolgingsplan.domain.*

fun generateRSHodemelding(): RSHodemelding {
    return RSHodemelding(
        meldingInfo = RSMeldingInfo(
            mottaker = RSMottaker(
                partnerId = "herId",
                herId = "partnerId",
                orgnummer = "orgnummer",
                navn = "navn",
                adresse = "adresse",
                postnummer = "postnummer",
                poststed = "poststed",
                behandler = RSBehandler(
                    fnr = "fnr",
                    hprId = 1,
                    fornavn = "Dana",
                    mellomnavn = "Katherine",
                    etternavn = "Scully",
                )
            ),
            pasient = RSPasient(
                fnr = "innbyggerFnr",
                fornavn = "Idun",
                mellomnavn = "mellomnavn",
                etternavn = "Innbygger",
            )
        ),
        vedlegg = RSVedlegg(
            vedlegg = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
        ),
    )
}
