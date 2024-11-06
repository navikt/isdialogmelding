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
    kilde = "isbehandlerdialog",
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
    kilde = "isbehandlerdialog",
)

fun generateDialogmeldingToBehandlerBestillingAvlysningDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = generateDialogmeldingToBehandlerBestillingHenvendelseNotat(
    behandlerRef = behandlerRef,
    uuid = uuid,
    arbeidstakerPersonident = arbeidstakerPersonident,
    kode = DialogmeldingKode.KODE4,
    tekst = "Møtet er avlyst",
)

fun generateDialogmeldingToBehandlerBestillingReferatDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = generateDialogmeldingToBehandlerBestillingHenvendelseNotat(
    behandlerRef = behandlerRef,
    uuid = uuid,
    arbeidstakerPersonident = arbeidstakerPersonident,
    kode = DialogmeldingKode.KODE9,
    tekst = "Dette er et referat",
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
    kilde = "isbehandlerdialog",
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
    kilde = "isbehandlerdialog",
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
    kilde = "isbehandlerdialog",
)

fun generateDialogmeldingToBehandlerBestillingNotatReturLegeerklaringDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = generateDialogmeldingToBehandlerBestillingHenvendelseNotat(
    behandlerRef = behandlerRef,
    uuid = uuid,
    arbeidstakerPersonident = arbeidstakerPersonident,
    kode = DialogmeldingKode.KODE3,
    tekst = "Dette er en henvendelse om retur av legeerklæring",
)

fun generateDialogmeldingToBehandlerBestillingNotatFriskmeldingTilArbeidsformidlingDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = generateDialogmeldingToBehandlerBestillingHenvendelseNotat(
    behandlerRef = behandlerRef,
    uuid = uuid,
    arbeidstakerPersonident = arbeidstakerPersonident,
    kode = DialogmeldingKode.KODE2,
    tekst = "Dette er en henvendelse om friskmelding til arbeidsformidling",
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
    kilde = "isbehandlerdialog",
)

fun generateDialogmeldingToBehandlerBestillingHenvendelseMeldingFraNavDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = generateDialogmeldingToBehandlerBestillingHenvendelseNotat(
    behandlerRef = behandlerRef,
    uuid = uuid,
    arbeidstakerPersonident = arbeidstakerPersonident,
    kode = DialogmeldingKode.KODE8,
    tekst = "Dette er en generell henvendelse fra NAV som ikke utløser takst",
)

private fun generateDialogmeldingToBehandlerBestillingHenvendelseNotat(
    behandlerRef: UUID,
    uuid: UUID,
    kode: DialogmeldingKode,
    tekst: String,
    arbeidstakerPersonident: Personident = Personident("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonident.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = DialogmeldingType.DIALOG_NOTAT.name,
    dialogmeldingKodeverk = DialogmeldingKodeverk.HENVENDELSE.name,
    dialogmeldingKode = kode.value,
    dialogmeldingTekst = tekst,
    dialogmeldingVedlegg = null,
    kilde = "isbehandlerdialog",
)
