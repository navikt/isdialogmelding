package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler.KafkaDialogmeldingFromBehandlerDTO
import java.time.LocalDateTime
import java.util.*

fun generateDialogmeldingFromBehandlerDTO(uuid: UUID) = KafkaDialogmeldingFromBehandlerDTO(
    msgId = uuid.toString(),
    navLogId = "1234asd123",
    mottattTidspunkt = LocalDateTime.now(),
    personIdentPasient = "",
    personIdentBehandler = "",
    legekontorOrgNr = "",
    legekontorHerId = "",
    legekontorOrgName = "",
    legehpr = "",
    fellesformatXML = "",
)
