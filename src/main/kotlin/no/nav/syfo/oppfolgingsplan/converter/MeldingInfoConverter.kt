package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgInfo
import no.nav.syfo.oppfolgingsplan.domain.RSMeldingInfo
import java.time.LocalDateTime
import java.util.*

fun createMsgInfo(
    msgId: String,
    rsMeldingInfo: RSMeldingInfo?,
): XMLMsgInfo {
    val factory = ObjectFactory()
    return factory.createXMLMsgInfo()
        .withType(
            factory.createXMLCS()
                .withDN("Notat")
                .withV("DIALOG_NOTAT")
        )
        .withMIGversion("v1.2 2006-05-24")
        .withGenDate(LocalDateTime.now())
        .withMsgId(msgId)
        .withAck(
            factory.createXMLCS()
                .withDN("Ja")
                .withV("J")
        )
        .withSender(createSender())
        .withReceiver(createReceiver(rsMeldingInfo?.mottaker))
        .withPatient(createPasient(rsMeldingInfo?.pasient))
}
