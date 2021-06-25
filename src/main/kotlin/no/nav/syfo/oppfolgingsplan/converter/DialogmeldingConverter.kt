package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.dialog._2006_10_11.ObjectFactory
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import java.util.*

class DialogmeldingConverter {
    private var dialogmelding: XMLDialogmelding? = null
    fun getDialogmelding(): XMLDialogmelding {
        ensureDialogmelding()
        return dialogmelding!!
    }

    private fun ensureDialogmelding() {
        if (dialogmelding == null) {
            dialogmelding = FACTORY.createXMLDialogmelding()
                .withNotat(
                    FACTORY.createXMLNotat()
                        .withTemaKodet(
                            FELLES_FACTORY.createXMLCV()
                                .withDN("Oppfølgingsplan")
                                .withS("2.16.578.1.12.4.1.1.8127")
                                .withV("1")
                        )
                        .withTekstNotatInnhold("Åpne PDF-vedlegg")
                        .withDokIdNotat(UUID.randomUUID().toString())
                        .withRollerRelatertNotat(
                            FACTORY.createXMLRollerRelatertNotat()
                                .withRolleNotat(
                                    FELLES_FACTORY.createXMLCV()
                                        .withS("2.16.578.1.12.4.1.1.9057")
                                        .withV("1")
                                )
                                .withPerson(FACTORY.createXMLPerson())
                        )
                )
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
        private val FELLES_FACTORY = no.kith.xmlstds.ObjectFactory()
    }
}
