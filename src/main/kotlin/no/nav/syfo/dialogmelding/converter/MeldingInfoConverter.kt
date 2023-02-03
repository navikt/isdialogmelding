package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgInfo
import no.nav.syfo.behandler.domain.*
import java.time.LocalDateTime

fun createMsgInfo(
    melding: DialogmeldingToBehandlerBestilling,
    arbeidstaker: Arbeidstaker,
): XMLMsgInfo {
    val factory = ObjectFactory()
    return factory.createXMLMsgInfo()
        .withType(
            factory.createXMLCS()
                .withDN(
                    if (
                        melding.type == DialogmeldingType.DIALOG_NOTAT ||
                        melding.type == DialogmeldingType.OPPFOLGINGSPLAN
                    ) "Notat" else "Forespørsel"
                )
                .withV(
                    if (
                        melding.type == DialogmeldingType.DIALOG_NOTAT ||
                        melding.type == DialogmeldingType.OPPFOLGINGSPLAN
                    ) DialogmeldingType.DIALOG_NOTAT.name else DialogmeldingType.DIALOG_FORESPORSEL.name
                )
        )
        .withMIGversion("v1.2 2006-05-24")
        .withGenDate(LocalDateTime.now())
        .withMsgId(melding.uuid.toString())
        .withAck(
            factory.createXMLCS()
                .withDN("Ja")
                .withV("J")
        ).also {
            if (melding.type != DialogmeldingType.OPPFOLGINGSPLAN) {
                it.withConversationRef(
                    factory.createXMLConversationRef()
                        .withRefToConversation(melding.conversationUuid.toString())
                        .withRefToParent(melding.parentRef ?: melding.conversationUuid.toString())
                )
            }
        }
        .withSender(createSender())
        .withReceiver(createReceiver(melding))
        .withPatient(createPasient(arbeidstaker))
}
