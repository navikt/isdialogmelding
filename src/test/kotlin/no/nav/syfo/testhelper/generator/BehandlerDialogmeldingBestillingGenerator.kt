package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.DialogmeldingKode
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.DialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.domain.PersonIdentNumber
import java.util.UUID

fun generateDialogmeldingToBehandlerBestillingDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonIdent: PersonIdentNumber = PersonIdentNumber("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonIdent.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = null,
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = "DIALOG_FORESPORSEL",
    dialogmeldingKode = DialogmeldingKode.INNKALLING.value,
    dialogmeldingTekst = "En tekst",
    dialogmeldingVedlegg = null,
)
fun generateDialogmeldingToBehandlerBestillingEndreTidStedDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonIdent: PersonIdentNumber = PersonIdentNumber("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonIdent.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = "DIALOG_FORESPORSEL",
    dialogmeldingKode = DialogmeldingKode.TIDSTED.value,
    dialogmeldingTekst = "Nytt tid og sted",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingAvlysningDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonIdent: PersonIdentNumber = PersonIdentNumber("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonIdent.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = "DIALOG_NOTAT",
    dialogmeldingKode = DialogmeldingKode.AVLYST.value,
    dialogmeldingTekst = "Møtet er avlyst",
    dialogmeldingVedlegg = null,
)

fun generateDialogmeldingToBehandlerBestillingReferatDTO(
    behandlerRef: UUID,
    uuid: UUID,
    arbeidstakerPersonIdent: PersonIdentNumber = PersonIdentNumber("01010112345"),
) = DialogmeldingToBehandlerBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = arbeidstakerPersonIdent.value,
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = uuid.toString(),
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = "DIALOG_NOTAT",
    dialogmeldingKode = DialogmeldingKode.REFERAT.value,
    dialogmeldingTekst = "Dette er et referat",
    dialogmeldingVedlegg = null,
)
