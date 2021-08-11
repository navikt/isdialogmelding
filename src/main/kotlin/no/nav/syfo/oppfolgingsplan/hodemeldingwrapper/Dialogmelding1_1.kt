package no.nav.syfo.oppfolgingsplan.hodemeldingwrapper

import no.kith.xmlstds.dialog._2013_01_23.XMLDialogmelding
import no.nav.syfo.oppfolgingsplan.hodemeldingwrapper.Dialogmelding.Versjon
import java.util.function.Function

class Dialogmelding1_1(dialogmelding1_1: XMLDialogmelding) : DialogmeldingAbstract() {
    override fun versjon(): Versjon? {
        return Versjon._1_1
    }

    init {
        dialogmelding1_1.notat.stream().map(Function { Notat1_1() }).forEach(notatListe::add)
        dialogmelding1_1.foresporsel.stream().map(Function { Foresporsel1_1() }).forEach(foresporselListe::add)
    }
}
