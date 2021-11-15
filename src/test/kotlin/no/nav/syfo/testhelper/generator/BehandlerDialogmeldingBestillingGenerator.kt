package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.kafka.BehandlerDialogmeldingBestillingDTO
import java.util.UUID

fun generateBehandlerDialogmeldingBestillingDTO(behandlerRef: UUID, uuid: UUID) = BehandlerDialogmeldingBestillingDTO(
    behandlerRef = behandlerRef.toString(),
    personIdent = "01010112345",
    dialogmeldingUuid = uuid.toString(),
    dialogmeldingRefParent = null,
    dialogmeldingRefConversation = uuid.toString(),
    dialogmeldingType = "DIALOG_FORESPORSEL",
    dialogmeldingKode = 1,
    dialogmeldingTekst = "En tekst",
    dialogmeldingVedlegg = null,
)
