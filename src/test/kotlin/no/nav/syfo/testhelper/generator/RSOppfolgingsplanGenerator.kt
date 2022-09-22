package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.api.person.RSOppfolgingsplan
import no.nav.syfo.domain.Personident
import no.nav.syfo.testhelper.UserConstants

fun generateRSOppfolgingsplan(
    arbeidstakerPersonIdent: Personident = UserConstants.ARBEIDSTAKER_FNR,
): RSOppfolgingsplan {
    return RSOppfolgingsplan(
        sykmeldtFnr = arbeidstakerPersonIdent.value,
        oppfolgingsplanPdf = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    )
}
