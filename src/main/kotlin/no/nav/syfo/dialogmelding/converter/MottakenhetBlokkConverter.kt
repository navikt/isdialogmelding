package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import no.nav.syfo.behandler.domain.DialogmeldingType
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLMottakenhetBlokk

fun createMottakenhetBlokk(
    melding: BehandlerDialogmeldingBestilling,
): XMLMottakenhetBlokk {
    val factory = ObjectFactory()
    return factory.createXMLMottakenhetBlokk()
        .withPartnerReferanse(melding.behandler.partnerId.toString())
        .withEbRole("Saksbehandler")
        .withEbService(if (melding.type == DialogmeldingType.DIALOG_NOTAT) "HenvendelseFraSaksbehandler" else "DialogmoteInnkalling")
        .withEbAction(if (melding.type == DialogmeldingType.DIALOG_NOTAT) "Henvendelse" else "MoteInnkalling")
}
