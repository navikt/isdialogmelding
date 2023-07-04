package no.nav.syfo.util

import com.migesok.jaxb.adapter.javatime.LocalDateTimeXmlAdapter
import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter
import no.kith.xmlstds.base64container.XMLBase64Container
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.kith.xmlstds.msghead._2006_05_24.XMLMsgHead
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.helse.eiFellesformat2.XMLSporinformasjonBlokkType
import no.nav.xml.eiff._2.XMLEIFellesformat
import java.io.StringWriter
import javax.xml.bind.*
import javax.xml.stream.XMLStreamReader
import javax.xml.transform.stream.StreamResult

val apprecJaxBContext: JAXBContext = JAXBContext.newInstance(
    XMLEIFellesformat::class.java,
    XMLAppRec::class.java,
)

val apprecUnmarshaller: Unmarshaller = apprecJaxBContext.createUnmarshaller().apply {
    setAdapter(LocalDateTimeXmlAdapter::class.java, XMLDateTimeAdapter())
    setAdapter(LocalDateXmlAdapter::class.java, XMLDateAdapter())
}

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

    inline fun <reified T> unmarshallObject(xmlStreamReader: XMLStreamReader): T {
        val jaxbContext: JAXBContext = JAXBContext.newInstance(T::class.java)
        val unmarshaller: Unmarshaller = jaxbContext.createUnmarshaller()

        return unmarshaller.unmarshal(xmlStreamReader) as T
    }

    init {
        try {
            DIALOGMELDING_CONTEXT_1_0 = JAXBContext.newInstance(
                XMLEIFellesformat::class.java,
                XMLMsgHead::class.java,
                XMLDialogmelding::class.java,
                XMLBase64Container::class.java,
                XMLSporinformasjonBlokkType::class.java,
            )
        } catch (e: JAXBException) {
            throw RuntimeException(e)
        }
    }
}
