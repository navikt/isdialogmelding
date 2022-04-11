package no.nav.syfo.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

@PublishedApi
internal val log: Logger = LoggerFactory.getLogger("no.nav.syfo.util.XmlUtil")

@PublishedApi
internal inline fun <reified T> getUnmarshalledObject(xmlStreamReader: XMLStreamReader, localName: String): T {
    while (xmlStreamReader.hasNext()) {
        if (xmlStreamReader.isStartElement && xmlStreamReader.localName == localName) {
            break
        }
        xmlStreamReader.next()
    }

    return JAXB.unmarshallObject(xmlStreamReader)
}

inline fun <reified T> getObjectFromXmlString(xml: String, localName: String): T {
    val reader = StringReader(xml)

    reader.use {
        val xmlInputFactory = XMLInputFactory.newFactory()
        val xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader)

        try {
            return getUnmarshalledObject(xmlStreamReader, localName)
        } catch (e: Exception) {
            log.warn("Fikk en feil ved lesing av xml med XmlStreamReader ${e.message}")
            throw RuntimeException(e)
        } finally {
            xmlStreamReader.close()
        }
    }
}
