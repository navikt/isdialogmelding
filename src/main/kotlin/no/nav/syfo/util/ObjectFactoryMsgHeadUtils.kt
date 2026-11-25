package no.nav.syfo.util

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLIdent
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.isDNR

const val KODEVERK_FOR_ENHET_ID = "2.16.578.1.12.4.1.1.9051"
const val KODEVERK_FOR_PERSON_ID = "2.16.578.1.12.4.1.1.8116"
const val KODEVERK_FOR_HELSEPERSONELL_TYPE = "2.16.578.1.12.4.1.1.9034"
const val KODEVERK_FOR_ROLLE_NOTAT = "2.16.578.1.12.4.1.1.9057"

fun ObjectFactory.createXMLIdentForPersonident(personident: Personident): XMLIdent {
    val personidentIsDNR = personident.isDNR()
    return this.createXMLIdent()
        .withId(personident.value)
        .withTypeId(
            this.createXMLCV()
                .withDN(if (personidentIsDNR) "D-nummer" else "Fødselsnummer")
                .withS(KODEVERK_FOR_PERSON_ID)
                .withV(if (personidentIsDNR) "DNR" else "FNR")
        )
}
