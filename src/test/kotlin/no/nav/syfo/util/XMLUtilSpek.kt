package no.nav.syfo.util

import no.kith.xmlstds.dialog._2006_10_11.XMLNotat
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.*
import no.nav.xml.eiff._2.XMLMottakenhetBlokk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class XMLUtilSpek : Spek({

    describe("Perform actions on XML") {

        describe("getObjectFromXmlString") {
            it("return object of given class from an xml string") {
                val mottakenhetBlokk = getObjectFromXmlString<XMLMottakenhetBlokk>(fellesformatXml, "MottakenhetBlokk")
                val notat = getObjectFromXmlString<XMLNotat>(completeFellesformatXml, "Notat")

                mottakenhetBlokk.partnerReferanse shouldBeEqualTo UserConstants.PARTNERID.toString()
                notat.dokIdNotat shouldBeEqualTo dokIdNotat
            }
            it("throw exception if localName doesn't match class") {
                assertThrows(RuntimeException::class.java) {
                    getObjectFromXmlString<XMLMottakenhetBlokk>(completeFellesformatXml, "partnerReferanse")
                }
            }
            it("throw exception if wanted tag isn't found") {
                assertThrows(RuntimeException::class.java) {
                    getObjectFromXmlString<NotXmlClass>(completeFellesformatXml, "NotXmlClass")
                }
            }
        }
    }
})

data class NotXmlClass(
    val value: String,
)
