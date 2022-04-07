package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjon

fun createPasient(
    arbeidstaker: BehandlerArbeidstakerRelasjon,
): XMLPatient {
    val factory = ObjectFactory()
    return factory.createXMLPatient()
        .withFamilyName(arbeidstaker.etternavn)
        .withMiddleName(arbeidstaker.mellomnavn)
        .withGivenName(arbeidstaker.fornavn)
        .withIdent(
            factory.createXMLIdent()
                .withId(arbeidstaker.arbeidstakerPersonident.value)
                .withTypeId(
                    factory.createXMLCV()
                        .withDN("Fødselsnummer")
                        .withS("2.16.578.1.12.4.1.1.8116")
                        .withV("FNR")
                )
        )
}
