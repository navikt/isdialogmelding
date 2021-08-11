package no.nav.syfo.util

import no.kith.xmlstds.base64container.XMLBase64Container
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.xml.eiff._2.XMLEIFellesformat
import java.io.StringWriter
import javax.xml.bind.*
import javax.xml.transform.stream.StreamResult

object JAXB {
    private var DIALOGMELDING_CONTEXT_1_0: JAXBContext? = null

    fun marshallDialogmelding1_0(element: Any?): String {
        return try {
            val writer = StringWriter()
            val marshaller = DIALOGMELDING_CONTEXT_1_0!!.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8")
            marshaller.marshal(element, StreamResult(writer))
            writer.toString()
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }
    }

    init {
        try {
            DIALOGMELDING_CONTEXT_1_0 = JAXBContext.newInstance(
                XMLEIFellesformat::class.java,
                XMLMsgHead::class.java,
                XMLDialogmelding::class.java,
                XMLBase64Container::class.java
            )
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }
    }
}
