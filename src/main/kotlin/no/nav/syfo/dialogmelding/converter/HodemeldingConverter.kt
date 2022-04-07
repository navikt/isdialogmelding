package no.nav.syfo.dialogmelding.converter
import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjon
import no.nav.syfo.behandler.domain.DialogmeldingToBehandlerBestilling

fun createMsgHead(
    melding: DialogmeldingToBehandlerBestilling,
    arbeidstaker: BehandlerArbeidstakerRelasjon,
): XMLMsgHead {
    val factory = ObjectFactory()
    return factory.createXMLMsgHead()
        .withMsgInfo(createMsgInfo(melding, arbeidstaker))
        .withDocument(createDialogmeldingDocument(melding))
        .withDocument(createVedleggDocument(melding))
}
