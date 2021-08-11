package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLDocument
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DokumentDialogmeldingConverter {
    private val dialogmeldingConverter: DialogmeldingConverter
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
                        .withDN("Hoveddokument")
                        .withV("H")
                )
                .withRefDoc(
                    FACTORY.createXMLRefDoc()
                        .withIssueDate(
                            FACTORY.createXMLTS()
                                .withV(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                        )
                        .withMsgType(
                            FACTORY.createXMLCS()
                                .withDN("XML-instans")
                                .withV("XML")
                        )
                        .withMimeType("text/xml")
                        .withContent(
                            FACTORY.createXMLRefDocContent()
                                .withAny(dialogmeldingConverter.getDialogmelding())
                        )
                )
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }

    init {
        dialogmeldingConverter = DialogmeldingConverter()
    }
}
