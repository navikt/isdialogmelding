package no.nav.syfo.fellesformat.hodemeldingwrapper

import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.nav.syfo.fellesformat.hodemeldingwrapper.Dialogmelding.Versjon
import java.util.function.Function

class Dialogmelding1_0(dialogmelding1_0: XMLDialogmelding) : DialogmeldingAbstract() {
    override fun versjon(): Versjon? {
        return Versjon._1_0
    }

    init {
        dialogmelding1_0.notat.stream().map(Function { Notat1_0() }).forEach(notatListe::add)
        dialogmelding1_0.foresporsel.stream().map(Function { Foresporsel1_0() }).forEach(foresporselListe::add)
    }
}
