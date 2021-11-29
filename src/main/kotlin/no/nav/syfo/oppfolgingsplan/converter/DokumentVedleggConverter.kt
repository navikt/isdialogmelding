package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.base64container.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import no.nav.syfo.oppfolgingsplan.domain.RSVedlegg
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun createVedleggDocument(
    rsVedlegg: RSVedlegg?,
): XMLDocument {
    val factory = no.kith.xmlstds.msghead._2006_05_24.ObjectFactory()
    val vedleggFactory = ObjectFactory()
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
                                .withValue(rsVedlegg!!.vedlegg)
                        )
                )
        )
}
