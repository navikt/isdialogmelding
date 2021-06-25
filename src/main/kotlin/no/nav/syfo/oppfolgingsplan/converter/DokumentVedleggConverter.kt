package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.base64container.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import no.nav.syfo.oppfolgingsplan.domain.RSVedlegg
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DokumentVedleggConverter(private val rsVedlegg: RSVedlegg?) {
    private var document: XMLDocument? = null
    fun getDocument(): XMLDocument {
        ensureDocument()
        return document!!
    }

    private fun ensureDocument() {
        if (document == null) {
            document = FACTORY.createXMLDocument()
                .withDocumentConnection(
                    FACTORY.createXMLCS()
                        .withDN("Vedlegg")
                        .withV("V")
                )
                .withRefDoc(
                    FACTORY.createXMLRefDoc()
                        .withIssueDate(
                            FACTORY.createXMLTS()
                                .withV(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                        )
                        .withMsgType(
                            FACTORY.createXMLCS()
                                .withDN("Vedlegg")
                                .withV("A")
                        )
                        .withMimeType("application/pdf")
                        .withContent(
                            FACTORY.createXMLRefDocContent()
                                .withAny(
                                    VEDLEGG_FACTORY.createXMLBase64Container()
                                        .withValue(rsVedlegg!!.vedlegg)
                                )
                        )
                )
        }
    }

    companion object {
        private val FACTORY = no.kith.xmlstds.msghead._2006_05_24.ObjectFactory()
        private val VEDLEGG_FACTORY = ObjectFactory()
    }
}
