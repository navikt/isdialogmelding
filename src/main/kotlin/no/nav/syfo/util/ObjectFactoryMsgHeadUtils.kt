package no.nav.syfo.util

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLIdent
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.isDNR

fun ObjectFactory.createXMLIdentForPersonident(personident: Personident): XMLIdent {
    val personidentIsDNR = personident.isDNR()
    return this.createXMLIdent()
        .withId(personident.value)
        .withTypeId(
            this.createXMLCV()
                .withDN(if (personidentIsDNR) "D-nummer" else "Fødselsnummer")
                .withS("2.16.578.1.12.4.1.1.8116")
                .withV(if (personidentIsDNR) "DNR" else "FNR")
        )
}
