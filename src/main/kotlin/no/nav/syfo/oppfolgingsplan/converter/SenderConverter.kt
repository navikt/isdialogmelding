package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLSender

class SenderConverter {
    private var sender: XMLSender? = null
    fun getSender(): XMLSender {
        ensureOrganisation()
        return sender!!
    }

    private fun ensureOrganisation() {
        if (sender == null) {
            sender = FACTORY.createXMLSender()
                .withOrganisation(
                    FACTORY.createXMLOrganisation()
                        .withOrganisationName("NAV")
                        .withIdent(
                            FACTORY.createXMLIdent()
                                .withId("889640782")
                                .withTypeId(
                                    FACTORY.createXMLCV()
                                        .withDN("Organisasjonsnummeret i Enhetsregisteret")
                                        .withS("2.16.578.1.12.4.1.1.9051")
                                        .withV("ENH")
                                )
                        )
                        .withIdent(
                            FACTORY.createXMLIdent()
                                .withId("79768")
                                .withTypeId(
                                    FACTORY.createXMLCV()
                                        .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                                        .withS("2.16.578.1.12.4.1.1.9051")
                                        .withV("HER")
                                )
                        )
                )
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }
}
