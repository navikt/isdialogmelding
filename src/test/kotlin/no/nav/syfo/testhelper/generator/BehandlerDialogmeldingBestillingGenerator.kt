package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.DialogmeldingKode
import no.nav.syfo.behandler.domain.DialogmeldingType
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.DialogmeldingToBehandlerBestillingDTO
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
    dialogmeldingKode = DialogmeldingKode.INNKALLING.value,
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
    dialogmeldingKode = DialogmeldingKode.TIDSTED.value,
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
    dialogmeldingKode = DialogmeldingKode.AVLYST.value,
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
    dialogmeldingKode = DialogmeldingKode.REFERAT.value,
    dialogmeldingTekst = "Dette er et referat",
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
    dialogmeldingKode = DialogmeldingKode.INNKALLING.value,
    dialogmeldingTekst = null,
    dialogmeldingVedlegg = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
)
