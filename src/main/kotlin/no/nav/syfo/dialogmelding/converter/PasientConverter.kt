package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker

fun createPasient(
    arbeidstakerNavn: BehandlerDialogmeldingArbeidstaker,
): XMLPatient {
    val factory = ObjectFactory()
    return factory.createXMLPatient()
        .withFamilyName(arbeidstakerNavn.etternavn)
        .withMiddleName(arbeidstakerNavn.mellomnavn)
        .withGivenName(arbeidstakerNavn.fornavn)
        .withIdent(
            factory.createXMLIdent()
                .withId(arbeidstakerNavn.arbeidstakerPersonident.value)
                .withTypeId(
                    factory.createXMLCV()
                        .withDN("Fødselsnummer")
                        .withS("2.16.578.1.12.4.1.1.8116")
                        .withV("FNR")
                )
        )
}
