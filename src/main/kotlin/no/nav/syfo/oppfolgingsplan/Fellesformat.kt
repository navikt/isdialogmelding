package no.nav.syfo.oppfolgingsplan

import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.syfo.oppfolgingsplan.hodemeldingwrapper.Hodemelding
import no.nav.xml.eiff._2.XMLEIFellesformat
import no.nav.xml.eiff._2.XMLMottakenhetBlokk
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Stream

class Fellesformat private constructor(val eIFellesformat: XMLEIFellesformat) {
    var message: String? = null
        private set
    private val mottakenhetBlokkListe: MutableList<XMLMottakenhetBlokk>
    private val hodemeldingListe: MutableList<Hodemelding>

    constructor(fellesformat: XMLEIFellesformat, message: String?) : this(fellesformat) {
        this.message = message
    }

    constructor(
        fellesformat: XMLEIFellesformat,
        marshaller: Function<XMLEIFellesformat?, String?>
    ) : this(fellesformat) {
        message = marshaller.apply(fellesformat)
    }

    val hodemeldingStream: Stream<Hodemelding>
        get() = hodemeldingListe.stream()

    fun erHodemelding(): Boolean {
        return hodemeldingStream.count() > 0
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
