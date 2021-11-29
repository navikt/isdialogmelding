package no.nav.syfo.oppfolgingsplan.converter

import no.nav.syfo.oppfolgingsplan.domain.RSHodemelding
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLMottakenhetBlokk

fun createMottakenhetBlokk(
    rsHodemelding: RSHodemelding,
): XMLMottakenhetBlokk {
    val factory = ObjectFactory()
    return factory.createXMLMottakenhetBlokk()
        .withPartnerReferanse(rsHodemelding.meldingInfo!!.mottaker!!.partnerId)
        .withEbRole("Saksbehandler")
        .withEbService("Oppfolgingsplan")
        .withEbAction("Plan")
}
