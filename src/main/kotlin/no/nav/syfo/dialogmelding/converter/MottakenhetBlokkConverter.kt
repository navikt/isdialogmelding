package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLMottakenhetBlokk

class MottakenhetBlokkConverter(private val melding: BehandlerDialogmeldingBestilling) {
    private var xmlMottakenhetBlokk: XMLMottakenhetBlokk? = null
    val mottakenhetBlokk: XMLMottakenhetBlokk
        get() {
            ensureMottakenhetBlokk()
            return xmlMottakenhetBlokk!!
        }

    private fun ensureMottakenhetBlokk() {
        if (xmlMottakenhetBlokk == null) {
            xmlMottakenhetBlokk = FACTORY.createXMLMottakenhetBlokk()
                .withPartnerReferanse(melding.behandler.partnerId.toString())
                .withEbRole("Saksbehandler")
                .withEbService("Dialogmelding")
                .withEbAction("Plan")
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }
}
