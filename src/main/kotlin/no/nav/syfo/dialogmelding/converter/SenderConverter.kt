package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLSender

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
                                .withS("2.16.578.1.12.4.1.1.9051")
                                .withV("ENH")
                        )
                )
                .withIdent(
                    factory.createXMLIdent()
                        .withId("79768")
                        .withTypeId(
                            factory.createXMLCV()
                                .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                                .withS("2.16.578.1.12.4.1.1.9051")
                                .withV("HER")
                        )
                )
        )
}
