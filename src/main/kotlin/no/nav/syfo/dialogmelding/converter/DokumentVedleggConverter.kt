package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun createVedleggDocument(
    melding: DialogmeldingToBehandlerBestilling,
): XMLDocument {
    val factory = ObjectFactory()
    val vedleggFactory = no.kith.xmlstds.base64container.ObjectFactory()
    return factory.createXMLDocument()
        .withDocumentConnection(
            factory.createXMLCS()
                .withDN("Vedlegg")
                .withV("V")
        )
        .withRefDoc(
            factory.createXMLRefDoc()
                .withIssueDate(
                    factory.createXMLTS()
                        .withV(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                )
                .withMsgType(
                    factory.createXMLCS()
                        .withDN("Vedlegg")
                        .withV("A")
                )
                .withMimeType("application/pdf")
                .withContent(
                    factory.createXMLRefDocContent()
                        .withAny(
                            vedleggFactory.createXMLBase64Container()
                                .withValue(melding.vedlegg)
                        )
                )
        )
}
