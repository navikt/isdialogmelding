package no.nav.syfo.oppfolgingsplan.hodemeldingwrapper

import java.util.stream.Stream

abstract class DialogmeldingAbstract internal constructor() : Dialogmelding {
    var notatListe: MutableList<Notat>
    var foresporselListe: MutableList<Foresporsel>
    override fun erForesporsel(): Boolean {
        return !foresporselListe.isEmpty()
    }

    override fun erNotat(): Boolean {
        return !notatListe.isEmpty()
    }

    override val dokIdForesporselStream: Stream<String?>?
        get() = foresporselListe.stream().map { obj: Foresporsel -> obj.dokIdForesporsel }
    override val dokIdNotatStream: Stream<String?>?
        get() = notatListe.stream().map { obj: Notat -> obj.dokIdNotat }

    init {
        notatListe = ArrayList()
        foresporselListe = ArrayList()
    }
}
