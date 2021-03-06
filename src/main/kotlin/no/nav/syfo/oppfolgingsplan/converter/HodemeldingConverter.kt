package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.syfo.oppfolgingsplan.domain.RSHodemelding

fun createMsgHead(
    rsHodemelding: RSHodemelding,
): XMLMsgHead {
    val factory = ObjectFactory()
    return factory.createXMLMsgHead()
        .withMsgInfo(createMsgInfo(rsMeldingInfo = rsHodemelding.meldingInfo))
        .withDocument(createDialogmeldingDocument())
        .withDocument(createVedleggDocument(rsVedlegg = rsHodemelding.vedlegg))
}
