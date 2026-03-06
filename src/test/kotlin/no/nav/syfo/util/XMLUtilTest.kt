package no.nav.syfo.util

import no.kith.xmlstds.dialog._2006_10_11.XMLNotat
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.*
import no.nav.xml.eiff._2.XMLMottakenhetBlokk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class XMLUtilTest {

    @Test
    fun `return object of given class from an xml string`() {
        val mottakenhetBlokk = getObjectFromXmlString<XMLMottakenhetBlokk>(fellesformatXml, "MottakenhetBlokk")
        val notat = getObjectFromXmlString<XMLNotat>(completeFellesformatXml, "Notat")

        assertEquals(UserConstants.PARTNERID.toString(), mottakenhetBlokk.partnerReferanse)
        assertEquals(dokIdNotat, notat.dokIdNotat)
    }

    @Test
    fun `throw exception if localName doesn't match class`() {
        assertThrows<RuntimeException> {
            getObjectFromXmlString<XMLMottakenhetBlokk>(completeFellesformatXml, "partnerReferanse")
        }
    }

    @Test
    fun `throw exception if wanted tag isn't found`() {
        assertThrows<RuntimeException> {
            getObjectFromXmlString<NotXmlClass>(completeFellesformatXml, "NotXmlClass")
        }
    }
}

data class NotXmlClass(
    val value: String,
)
