package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.createXMLIdentForPersonident
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
            rsPasient.fnr?.let {
                factory.createXMLIdentForPersonident(Personident(it))
            }
        )
}
