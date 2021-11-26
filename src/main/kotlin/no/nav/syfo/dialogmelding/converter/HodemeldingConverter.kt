package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling

class HodemeldingConverter(
    melding: BehandlerDialogmeldingBestilling,
    arbeidstakerNavn: BehandlerDialogmeldingArbeidstaker,
) {
    private val meldingInfoConverter: MeldingInfoConverter
    private val dokumentDialogmeldingConverter: DokumentDialogmeldingConverter
    private val dokumentVedleggConverter: DokumentVedleggConverter
    private var msgHead: XMLMsgHead? = null
    fun getMsgHead(): XMLMsgHead {
        ensureMsgHead()
        return msgHead!!
    }

    private fun ensureMsgHead() {
        if (msgHead == null) {
            msgHead = FACTORY.createXMLMsgHead()
                .withMsgInfo(meldingInfoConverter.getMsgInfo())
                .withDocument(dokumentDialogmeldingConverter.getDocument())
                .withDocument(dokumentVedleggConverter.getDocument())
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }

    init {
        meldingInfoConverter = MeldingInfoConverter(melding, arbeidstakerNavn)
        dokumentDialogmeldingConverter = DokumentDialogmeldingConverter(melding)
        dokumentVedleggConverter = DokumentVedleggConverter(melding)
    }
}
