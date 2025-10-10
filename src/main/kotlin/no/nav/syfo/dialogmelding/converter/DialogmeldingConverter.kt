package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.dialog._2006_10_11.ObjectFactory
import no.kith.xmlstds.dialog._2006_10_11.XMLDialogmelding
import no.nav.syfo.dialogmelding.bestilling.domain.*
import java.util.*

fun createDialogmelding(melding: DialogmeldingToBehandlerBestilling): XMLDialogmelding {
    val factory = ObjectFactory()
    val fellesFactory = no.kith.xmlstds.ObjectFactory()

    val kodeverk = melding.kodeverk
        ?: throw IllegalArgumentException("Cannot send dialogmelding when kodeverk is missing")
    val kode = melding.kode.value
    return if (melding.type == DialogmeldingType.DIALOG_NOTAT) {
        factory.createXMLDialogmelding()
            .withNotat(
                factory.createXMLNotat()
                    .withTemaKodet(
                        fellesFactory.createXMLCV()
                            .withDN(
                                when (kode) {
                                    2 -> "Friskmelding til arbeidsformidling"
                                    3 -> "Retur av legeerklæring"
                                    4 -> "Avlysning dialogmøte"
                                    8 -> "Melding fra NAV"
                                    9 -> "Informasjon fra NAV" // referat fra dialogmøte
                                    else -> throw IllegalArgumentException("Unsupported kode-value")
                                }
                            )
                            .withS(kodeverk.kodeverkId)
                            .withV(kode.toString())
                    )
                    .withTekstNotatInnhold(melding.getTekstRemoveInvalidCharacters())
                    .withDokIdNotat(UUID.randomUUID().toString())
            )
    } else if (melding.type == DialogmeldingType.OPPFOLGINGSPLAN) {
        factory.createXMLDialogmelding()
            .withNotat(
                factory.createXMLNotat()
                    .withTemaKodet(
                        fellesFactory.createXMLCV()
                            .withDN("Oppfølgingsplan")
                            .withS(kodeverk.kodeverkId)
                            .withV(kode.toString())
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
                                when (Pair(kodeverk, kode)) {
                                    Pair(DialogmeldingKodeverk.DIALOGMOTE, 1) -> "Innkalling dialogmøte 2"
                                    Pair(DialogmeldingKodeverk.DIALOGMOTE, 2) -> "Endring dialogmøte 2"
                                    Pair(DialogmeldingKodeverk.FORESPORSEL, 1) -> "Forespørsel om pasient"
                                    Pair(DialogmeldingKodeverk.FORESPORSEL, 2) -> "Påminnelse forespørsel om pasient"
                                    else -> throw IllegalArgumentException("Unsupported kodeverk/kode-values")
                                }
                            )
                            .withS(kodeverk.kodeverkId)
                            .withV(kode.toString())
                    )
                    .withSporsmal(melding.getTekstRemoveInvalidCharacters())
                    .withDokIdForesp(UUID.randomUUID().toString())
            )
    } else {
        throw IllegalArgumentException("Cannot send dialogmelding, unknown type: ${melding.type}")
    }
}
