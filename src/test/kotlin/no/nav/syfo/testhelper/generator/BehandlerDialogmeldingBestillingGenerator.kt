package no.nav.syfo.testhelper.generator

import no.nav.syfo.dialogmelding.bestilling.kafka.DialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.dialogmelding.bestilling.domain.*
import no.nav.syfo.domain.Personident
import java.util.UUID

fun generateDialogmeldingToBehandlerBestillingDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = null,
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_FORESPORSEL.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.DIALOGMOTE.name,
    dialogmeldingKode = DialogmeldingKode.KODE1.value,
    dialogmeldingTekst = "En tekst",
    dialogmeldingVedlegg = null,
)
fun generateDialogmeldingToBehandlerBestillingEndreTidStedDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_FORESPORSEL.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.DIALOGMOTE.name,
    dialogmeldingKode = DialogmeldingKode.KODE2.value,
    dialogmeldingTekst = "Nytt tid og sted",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingAvlysningDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_NOTAT.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.HENVENDELSE.name,
    dialogmeldingKode = DialogmeldingKode.KODE4.value,
    dialogmeldingTekst = "Møtet er avlyst",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingReferatDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_NOTAT.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.HENVENDELSE.name,
    dialogmeldingKode = DialogmeldingKode.KODE9.value,
    dialogmeldingTekst = "Dette er et referat",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingForesporselDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_FORESPORSEL.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.FORESPORSEL.name,
    dialogmeldingKode = DialogmeldingKode.KODE1.value,
    dialogmeldingTekst = "Dette er en forespørsel",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingForesporselPurringDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_FORESPORSEL.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.FORESPORSEL.name,
    dialogmeldingKode = DialogmeldingKode.KODE2.value,
    dialogmeldingTekst = "Dette er en påminnelse om en forespørsel",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingForesporselLegeerklaringDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_FORESPORSEL.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.FORESPORSEL.name,
    dialogmeldingKode = DialogmeldingKode.KODE1.value,
    dialogmeldingTekst = "Dette er en forespørsel om legeerklæring",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingNotatReturLegeerklæringDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_NOTAT.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.HENVENDELSE.name,
    dialogmeldingKode = DialogmeldingKode.KODE3.value,
    dialogmeldingTekst = "Dette er en henvendelse om retur av legeerklæring",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingOppfolgingsplanDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = null,
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.OPPFOLGINGSPLAN.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.HENVENDELSE.name,
    dialogmeldingKode = DialogmeldingKode.KODE1.value,
    dialogmeldingTekst = null,
    dialogmeldingVedlegg = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
)

fun generateDialogmeldingToBehandlerBestillingHenvendelseMeldingFraNavDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_NOTAT.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.HENVENDELSE.name,
    dialogmeldingKode = DialogmeldingKode.KODE8.value,
    dialogmeldingTekst = "Dette er en generell henvendelse fra NAV som ikke utløser takst",
    dialogmeldingVedlegg = null,
)
