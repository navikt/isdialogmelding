package no.nav.syfo.fellesformat

import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.syfo.fellesformat.hodemeldingwrapper.Hodemelding
import no.nav.xml.eiff._2.XMLEIFellesformat
import no.nav.xml.eiff._2.XMLMottakenhetBlokk
import java.util.function.Consumer
import java.util.function.Function

class Fellesformat private constructor(val eIFellesformat: XMLEIFellesformat) {
    var message: String? = null
        private set
    private val mottakenhetBlokkListe: MutableList<XMLMottakenhetBlokk>
    private val hodemeldingListe: MutableList<Hodemelding>

    constructor(
        fellesformat: XMLEIFellesformat,
        marshaller: Function<XMLEIFellesformat?, String?>
    ) : this(fellesformat) {
        message = marshaller.apply(fellesformat)
    }

    init {
        mottakenhetBlokkListe = ArrayList()
        hodemeldingListe = ArrayList()

        eIFellesformat.any.forEach(
            Consumer { melding: Any? ->
                if (melding is XMLMsgHead) {
                    hodemeldingListe.add(Hodemelding(melding))
                } else if (melding is XMLMottakenhetBlokk) {
                    mottakenhetBlokkListe.add(melding)
                }
            }
        )
    }
}
