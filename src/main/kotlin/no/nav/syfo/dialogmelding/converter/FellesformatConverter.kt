package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLEIFellesformat

fun createFellesformat(
    melding: DialogmeldingToBehandlerBestilling,
    arbeidstaker: Arbeidstaker,
): XMLEIFellesformat {
    val factory = ObjectFactory()
    return factory.createXMLEIFellesformat()
        .withAny(createMsgHead(melding, arbeidstaker))
        .withAny(createMottakenhetBlokk(melding))
}
