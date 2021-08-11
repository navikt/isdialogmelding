package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgInfo
import no.nav.syfo.oppfolgingsplan.domain.RSMeldingInfo
import java.time.LocalDateTime
import java.util.*

class MeldingInfoConverter(rsMeldingInfo: RSMeldingInfo?) {
    private val senderConverter: SenderConverter
    private val mottakerConverter: MottakerConverter
    private val pasientConverter: PasientConverter
    private var msgInfo: XMLMsgInfo? = null
    fun getMsgInfo(): XMLMsgInfo {
        ensureMsgInfo()
        return msgInfo!!
    }

    private fun ensureMsgInfo() {
        if (msgInfo == null) {
            msgInfo = FACTORY.createXMLMsgInfo()
                .withType(
                    FACTORY.createXMLCS()
                        .withDN("Notat")
                        .withV("DIALOG_NOTAT")
                )
                .withMIGversion("v1.2 2006-05-24")
                .withGenDate(LocalDateTime.now())
                .withMsgId(UUID.randomUUID().toString())
                .withAck(
                    FACTORY.createXMLCS()
                        .withDN("Ja")
                        .withV("J")
                )
                .withSender(senderConverter.getSender())
                .withReceiver(mottakerConverter.getReceiver())
                .withPatient(pasientConverter.getPatient())
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }

    init {
        senderConverter = SenderConverter()
        mottakerConverter = MottakerConverter(rsMeldingInfo!!.mottaker)
        pasientConverter = PasientConverter(rsMeldingInfo.pasient)
    }
}
