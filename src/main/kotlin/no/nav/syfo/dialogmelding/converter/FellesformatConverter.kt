package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLEIFellesformat

class FellesformatConverter(
    melding: BehandlerDialogmeldingBestilling,
    arbeidstakerNavn: BehandlerDialogmeldingArbeidstaker,
) {
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
        hodemeldingConverter = HodemeldingConverter(melding, arbeidstakerNavn)
        mottakenhetBlokkConverter = MottakenhetBlokkConverter(melding)
    }
}
