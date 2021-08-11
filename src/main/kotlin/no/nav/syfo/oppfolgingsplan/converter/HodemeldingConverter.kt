package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.syfo.oppfolgingsplan.domain.RSHodemelding

class HodemeldingConverter(rsHodemelding: RSHodemelding) {
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
        meldingInfoConverter = MeldingInfoConverter(rsHodemelding.meldingInfo)
        dokumentDialogmeldingConverter = DokumentDialogmeldingConverter()
        dokumentVedleggConverter = DokumentVedleggConverter(rsHodemelding.vedlegg)
    }
}
