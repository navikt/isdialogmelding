package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.dialog._2006_10_11.ObjectFactory
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.nav.syfo.behandler.domain.*
import java.lang.RuntimeException
import java.util.*

fun createDialogmelding(melding: DialogmeldingToBehandlerBestilling): XMLDialogmelding {
    val factory = ObjectFactory()
    val fellesFactory = no.kith.xmlstds.ObjectFactory()

    val kodeverk = melding.kodeverk
    val kode = melding.kode.value
    return if (melding.type == DialogmeldingType.DIALOG_NOTAT) {
        factory.createXMLDialogmelding()
            .withNotat(
                factory.createXMLNotat()
                    .withTemaKodet(
                        fellesFactory.createXMLCV()
                            .withDN(if (kode == 4) "Avlysning dialogmøte" else "Informasjon fra NAV")
                            .withS(kodeverk?.kodeverkId ?: "2.16.578.1.12.4.1.1.8127")
                            .withV(kode.toString())
                    )
                    .withTekstNotatInnhold(melding.tekst)
                    .withDokIdNotat(UUID.randomUUID().toString())
            )
    } else if (melding.type == DialogmeldingType.OPPFOLGINGSPLAN) {
        factory.createXMLDialogmelding()
            .withNotat(
                factory.createXMLNotat()
                    .withTemaKodet(
                        fellesFactory.createXMLCV()
                            .withDN("Oppfølgingsplan")
                            .withS(kodeverk?.kodeverkId ?: "2.16.578.1.12.4.1.1.8127")
                            .withV("1")
                    )
                    .withTekstNotatInnhold("Åpne PDF-vedlegg")
                    .withDokIdNotat(UUID.randomUUID().toString())
                    .withRollerRelatertNotat(
                        factory.createXMLRollerRelatertNotat()
                            .withRolleNotat(
                                fellesFactory.createXMLCV()
                                    .withS("2.16.578.1.12.4.1.1.9057")
                                    .withV("1")
                            )
                            .withPerson(factory.createXMLPerson())
                    )
            )
    } else if (melding.type == DialogmeldingType.DIALOG_FORESPORSEL) {
        factory.createXMLDialogmelding()
            .withForesporsel(
                factory.createXMLForesporsel()
                    .withTypeForesp(
                        fellesFactory.createXMLCV()
                            .withDN(
                                if ((kodeverk == null ||kodeverk == DialogmeldingKodeverk.DIALOGMOTE) && kode == 1) {
                                    "Innkalling dialogmøte 2"
                                } else if ((kodeverk == null || kodeverk == DialogmeldingKodeverk.DIALOGMOTE) && kode == 2) {
                                    "Endring dialogmøte 2"
                                } else if (kodeverk == DialogmeldingKodeverk.FORESPORSEL && kode == 1) {
                                    "Forespørsel om pasient"
                                } else if (kodeverk == DialogmeldingKodeverk.FORESPORSEL && kode == 2) {
                                    "Påminnelse forespørsel om pasient"
                                } else {
                                    throw RuntimeException("Unsupported kodeverk/kode")
                                }
                            )
                            .withS(kodeverk?.kodeverkId ?: "2.16.578.1.12.4.1.1.8125")
                            .withV(kode.toString())
                    )
                    .withSporsmal(melding.tekst)
                    .withDokIdForesp(UUID.randomUUID().toString())
            )
    } else {
        throw RuntimeException("Unsupported type/kodeverk")
    }
}
