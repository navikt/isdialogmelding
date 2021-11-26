package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.dialog._2006_10_11.ObjectFactory
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import java.util.*

fun createDialogmelding(): XMLDialogmelding {
    val factory = ObjectFactory()
    val fellesFactory = no.kith.xmlstds.ObjectFactory()
    return factory.createXMLDialogmelding()
        .withNotat(
            factory.createXMLNotat()
                .withTemaKodet(
                    fellesFactory.createXMLCV()
                        .withDN("Oppfølgingsplan")
                        .withS("2.16.578.1.12.4.1.1.8127")
                        .withV("1")
                )
                .withTekstNotatInnhold("Åpne PDF-vedlegg")
                .withDokIdNotat(UUID.randomUUID().toString())
                .withRollerRelatertNotat(
                    factory.createXMLRollerRelatertNotat()
                        .withRolleNotat(
                            fellesFactory.createXMLCV()
                                .withS("2.16.578.1.12.4.1.1.9057")
                                .withV("1")
                        )
                        .withPerson(factory.createXMLPerson())
                )
        )
}
