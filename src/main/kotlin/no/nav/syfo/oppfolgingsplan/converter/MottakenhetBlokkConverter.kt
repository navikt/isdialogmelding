package no.nav.syfo.oppfolgingsplan.converter

import no.nav.syfo.oppfolgingsplan.domain.RSHodemelding
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLMottakenhetBlokk

class MottakenhetBlokkConverter(private val rsHodemelding: RSHodemelding) {
    private var xmlMottakenhetBlokk: XMLMottakenhetBlokk? = null
    val mottakenhetBlokk: XMLMottakenhetBlokk
        get() {
            ensureMottakenhetBlokk()
            return xmlMottakenhetBlokk!!
        }

    private fun ensureMottakenhetBlokk() {
        if (xmlMottakenhetBlokk == null) {
            xmlMottakenhetBlokk = FACTORY.createXMLMottakenhetBlokk()
                .withPartnerReferanse(rsHodemelding.meldingInfo!!.mottaker!!.partnerId)
                .withEbRole("Saksbehandler")
                .withEbService("Oppfolgingsplan")
                .withEbAction("Plan")
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }
}
