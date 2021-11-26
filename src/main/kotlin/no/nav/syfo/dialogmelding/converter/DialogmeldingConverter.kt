package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.dialog._2006_10_11.ObjectFactory
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import no.nav.syfo.behandler.domain.DialogmeldingType
import java.util.*

class DialogmeldingConverter(private val melding: BehandlerDialogmeldingBestilling) {
    private var dialogmelding: XMLDialogmelding? = null
    fun getDialogmelding(): XMLDialogmelding {
        ensureDialogmelding()
        return dialogmelding!!
    }

    private fun ensureDialogmelding() {
        if (dialogmelding == null) {
            val kode = melding.kode.value
            if (melding.type == DialogmeldingType.DIALOG_NOTAT) {
                dialogmelding = FACTORY.createXMLDialogmelding()
                    .withNotat(
                        FACTORY.createXMLNotat()
                            .withTemaKodet(
                                FELLES_FACTORY.createXMLCV()
                                    .withDN(if (kode == 4) "Avlysning dialogmøte" else "Informasjon fra NAV")
                                    .withS("2.16.578.1.12.4.1.1.8127")
                                    .withV(kode.toString())
                            )
                            .withTekstNotatInnhold(melding.tekst)
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
            } else { // melding.type == DialogmeldingType.DIALOG_FORESPORSEL
                dialogmelding = FACTORY.createXMLDialogmelding()
                    .withForesporsel(
                        FACTORY.createXMLForesporsel()
                            .withTypeForesp(
                                FELLES_FACTORY.createXMLCV()
                                    .withDN(if (kode == 1) "Innkalling dialogmøte 2" else "Endring dialogmøte 2")
                                    .withS("2.16.578.1.12.4.1.1.8125")
                                    .withV(kode.toString())
                            )
                            .withSporsmal(melding.tekst)
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
    }

    companion object {
        private val FACTORY = ObjectFactory()
        private val FELLES_FACTORY = no.kith.xmlstds.ObjectFactory()
    }
}
