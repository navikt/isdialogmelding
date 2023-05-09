package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.dialogmelding.bestilling.domain.*
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLMottakenhetBlokk

fun createMottakenhetBlokk(
    melding: DialogmeldingToBehandlerBestilling,
): XMLMottakenhetBlokk {
    val factory = ObjectFactory()
    return factory.createXMLMottakenhetBlokk()
        .withPartnerReferanse(melding.behandler.kontor.partnerId.toString())
        .withEbRole("Saksbehandler")
        .withEbService(
            when (Pair(melding.type, melding.kodeverk)) {
                Pair(DialogmeldingType.DIALOG_NOTAT, DialogmeldingKodeverk.HENVENDELSE) -> "HenvendelseFraSaksbehandler"
                Pair(DialogmeldingType.OPPFOLGINGSPLAN, DialogmeldingKodeverk.HENVENDELSE) -> "Oppfolgingsplan"
                Pair(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.DIALOGMOTE) -> "DialogmoteInnkalling"
                Pair(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.FORESPORSEL) -> "ForesporselFraSaksbehandler"
                else -> throw IllegalArgumentException("Invalid melding type/kodeverk-combination")
            }
        )
        .withEbAction(
            when (Pair(melding.type, melding.kodeverk)) {
                Pair(DialogmeldingType.DIALOG_NOTAT, DialogmeldingKodeverk.HENVENDELSE) -> "Henvendelse"
                Pair(DialogmeldingType.OPPFOLGINGSPLAN, DialogmeldingKodeverk.HENVENDELSE) -> "Plan"
                Pair(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.DIALOGMOTE) -> "MoteInnkalling"
                Pair(DialogmeldingType.DIALOG_FORESPORSEL, DialogmeldingKodeverk.FORESPORSEL) -> "Foresporsel"
                else -> throw IllegalArgumentException("Invalid melding type/kodeverk-combination")
            }
        )
}
