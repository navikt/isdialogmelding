package no.nav.syfo.oppfolgingsplan.converter

import no.nav.syfo.oppfolgingsplan.domain.RSHodemelding
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLEIFellesformat

class FellesformatConverter(rsHodemelding: RSHodemelding) {
    private val hodemeldingConverter: HodemeldingConverter
    private val mottakenhetBlokkConverter: MottakenhetBlokkConverter
    private var eiFellesformat: XMLEIFellesformat? = null
    fun getEiFellesformat(): XMLEIFellesformat {
        ensureFellesformat()
        return eiFellesformat!!
    }

    private fun ensureFellesformat() {
        if (eiFellesformat == null) {
            eiFellesformat = FACTORY.createXMLEIFellesformat()
                .withAny(hodemeldingConverter.getMsgHead())
                .withAny(mottakenhetBlokkConverter.mottakenhetBlokk)
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }

    init {
        hodemeldingConverter = HodemeldingConverter(rsHodemelding)
        mottakenhetBlokkConverter = MottakenhetBlokkConverter(rsHodemelding)
    }
}
