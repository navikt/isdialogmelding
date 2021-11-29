package no.nav.syfo.oppfolgingsplan.converter

import no.nav.syfo.oppfolgingsplan.domain.RSHodemelding
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLEIFellesformat

fun createFellesformat(
    rsHodemelding: RSHodemelding,
): XMLEIFellesformat {
    val factory = ObjectFactory()
    return factory.createXMLEIFellesformat()
        .withAny(createMsgHead(rsHodemelding = rsHodemelding))
        .withAny(createMottakenhetBlokk(rsHodemelding = rsHodemelding))
}
