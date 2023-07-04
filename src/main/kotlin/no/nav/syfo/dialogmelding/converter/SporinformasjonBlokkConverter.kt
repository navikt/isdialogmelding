package no.nav.syfo.dialogmelding.converter

import no.nav.helse.eiFellesformat2.ObjectFactory
import no.nav.helse.eiFellesformat2.XMLSporinformasjonBlokkType
import java.math.BigInteger
import java.util.GregorianCalendar
import javax.xml.bind.JAXBElement
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory

fun createSporinformasjonBlokk(): JAXBElement<XMLSporinformasjonBlokkType> {
    val factory = ObjectFactory()
    val datetypeFactory = DatatypeFactory.newInstance()
    val xmlSporinformasjonBlokkType = factory.createXMLSporinformasjonBlokkType().apply {
        programID = "MODIA SYFO"
        programVersjonID = "1.0"
        programResultatKoder = BigInteger.ZERO
        tidsstempel = datetypeFactory.newXMLGregorianCalendar(GregorianCalendar()).apply {
            millisecond = DatatypeConstants.FIELD_UNDEFINED
            timezone = DatatypeConstants.FIELD_UNDEFINED
        }
    }
    return factory.createSporinformasjonBlokk(xmlSporinformasjonBlokkType)
}
