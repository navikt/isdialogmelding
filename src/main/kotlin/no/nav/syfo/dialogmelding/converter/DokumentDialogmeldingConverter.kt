package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun createDialogmeldingDocument(melding: BehandlerDialogmeldingBestilling): XMLDocument {
    val factory = ObjectFactory()
    return factory.createXMLDocument()
        .withDocumentConnection(
            factory.createXMLCS()
                .withDN("Hoveddokument")
                .withV("H")
        )
        .withRefDoc(
            factory.createXMLRefDoc()
                .withIssueDate(
                    factory.createXMLTS()
                        .withV(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                )
                .withMsgType(
                    factory.createXMLCS()
                        .withDN("XML-instans")
                        .withV("XML")
                )
                .withMimeType("text/xml")
                .withContent(
                    factory.createXMLRefDocContent()
                        .withAny(createDialogmelding(melding))
                )
        )
}
