package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.dialog._2006_10_11.ObjectFactory
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.nav.syfo.behandler.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.behandler.domain.DialogmeldingType
import java.util.*

fun createDialogmelding(melding: DialogmeldingToBehandlerBestilling): XMLDialogmelding {
    val factory = ObjectFactory()
    val fellesFactory = no.kith.xmlstds.ObjectFactory()

    val kode = melding.kode.value
    return if (melding.type == DialogmeldingType.DIALOG_NOTAT) {
        factory.createXMLDialogmelding()
            .withNotat(
                factory.createXMLNotat()
                    .withTemaKodet(
                        fellesFactory.createXMLCV()
                            .withDN(if (kode == 4) "Avlysning dialogmøte" else "Informasjon fra NAV")
                            .withS("2.16.578.1.12.4.1.1.8127")
                            .withV(kode.toString())
                    )
                    .withTekstNotatInnhold(melding.tekst)
                    .withDokIdNotat(UUID.randomUUID().toString())
            )
    } else { // melding.type == DialogmeldingType.DIALOG_FORESPORSEL
        factory.createXMLDialogmelding()
            .withForesporsel(
                factory.createXMLForesporsel()
                    .withTypeForesp(
                        fellesFactory.createXMLCV()
                            .withDN(if (kode == 1) "Innkalling dialogmøte 2" else "Endring dialogmøte 2")
                            .withS("2.16.578.1.12.4.1.1.8125")
                            .withV(kode.toString())
                    )
                    .withSporsmal(melding.tekst)
                    .withDokIdForesp(UUID.randomUUID().toString())
            )
    }
}
