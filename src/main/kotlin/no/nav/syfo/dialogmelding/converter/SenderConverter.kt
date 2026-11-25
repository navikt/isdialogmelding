package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLSender
import no.nav.syfo.util.KODEVERK_FOR_ENHET_ID

fun createSender(): XMLSender {
    val factory = ObjectFactory()
    return factory.createXMLSender()
        .withOrganisation(
            factory.createXMLOrganisation()
                .withOrganisationName("NAV")
                .withIdent(
                    factory.createXMLIdent()
                        .withId("889640782")
                        .withTypeId(
                            factory.createXMLCV()
                                .withDN("Organisasjonsnummeret i Enhetsregisteret")
                                .withS(KODEVERK_FOR_ENHET_ID)
                                .withV("ENH")
                        )
                )
                .withIdent(
                    factory.createXMLIdent()
                        .withId("79768")
                        .withTypeId(
                            factory.createXMLCV()
                                .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                                .withS(KODEVERK_FOR_ENHET_ID)
                                .withV("HER")
                        )
                )
        )
}
