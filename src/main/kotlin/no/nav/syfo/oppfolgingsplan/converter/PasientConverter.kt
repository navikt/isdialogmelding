package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.nav.syfo.oppfolgingsplan.domain.RSPasient

fun createPasient(
    rsPasient: RSPasient?,
): XMLPatient {
    val factory = ObjectFactory()
    return factory.createXMLPatient()
        .withFamilyName(rsPasient!!.etternavn)
        .withMiddleName(rsPasient.mellomnavn)
        .withGivenName(rsPasient.fornavn)
        .withIdent(
            factory.createXMLIdent()
                .withId(rsPasient.fnr)
                .withTypeId(
                    factory.createXMLCV()
                        .withDN("Fødselsnummer")
                        .withS("2.16.578.1.12.4.1.1.8116")
                        .withV("FNR")
                )
        )
}
