package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.dialogmelding.bestilling.domain.*
import no.nav.syfo.domain.PartnerId
import no.nav.xml.eiff._2.ObjectFactory
import no.nav.xml.eiff._2.XMLMottakenhetBlokk

fun createMottakenhetBlokk(
    melding: DialogmeldingToBehandlerBestilling,
): XMLMottakenhetBlokk {
    val factory = ObjectFactory()
    val storedPartnerId = melding.behandler.kontor.partnerId
    val partnerId = if (storedPartnerId.value == 14859 || storedPartnerId.value == 41578) PartnerId(60274) else storedPartnerId
    return factory.createXMLMottakenhetBlokk()
        .withPartnerReferanse(partnerId.toString())
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
