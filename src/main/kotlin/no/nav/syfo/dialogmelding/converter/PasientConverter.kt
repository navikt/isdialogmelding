package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.nav.syfo.behandler.domain.Arbeidstaker
import no.nav.syfo.util.createXMLIdentForPersonident

fun createPasient(
    arbeidstaker: Arbeidstaker,
): XMLPatient {
    val factory = ObjectFactory()
    return factory.createXMLPatient()
        .withFamilyName(arbeidstaker.etternavn)
        .withMiddleName(arbeidstaker.mellomnavn)
        .withGivenName(arbeidstaker.fornavn)
        .withIdent(
            factory.createXMLIdentForPersonident(arbeidstaker.arbeidstakerPersonident)
        )
}
