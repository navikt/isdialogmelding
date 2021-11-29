package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLEIFellesformat

fun createFellesformat(
    melding: BehandlerDialogmeldingBestilling,
    arbeidstakerNavn: BehandlerDialogmeldingArbeidstaker,
): XMLEIFellesformat {
    val factory = ObjectFactory()
    return factory.createXMLEIFellesformat()
        .withAny(createMsgHead(melding, arbeidstakerNavn))
        .withAny(createMottakenhetBlokk(melding))
}
