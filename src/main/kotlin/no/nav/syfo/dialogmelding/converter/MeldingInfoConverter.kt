package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgInfo
import no.nav.syfo.behandler.domain.*
import java.time.LocalDateTime
import java.util.*

class MeldingInfoConverter(
    val melding: BehandlerDialogmeldingBestilling,
    arbeidstakerNavn: BehandlerDialogmeldingArbeidstaker,
) {
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
                        .withDN(if (melding.type == DialogmeldingType.DIALOG_NOTAT) "Notat" else "Forespørsel")
                        .withV(melding.type.name)
                )
                .withMIGversion("v1.2 2006-05-24")
                .withGenDate(LocalDateTime.now())
                .withMsgId(melding.uuid.toString())
                .withAck(
                    FACTORY.createXMLCS()
                        .withDN("Ja")
                        .withV("J")
                )
                .withConversationRef(
                    FACTORY.createXMLConversationRef()
                        .withRefToConversation(melding.conversationUuid.toString())
                        .withRefToParent(melding.parentUuid?.toString())
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
        mottakerConverter = MottakerConverter(melding)
        pasientConverter = PasientConverter(melding, arbeidstakerNavn)
    }
}
