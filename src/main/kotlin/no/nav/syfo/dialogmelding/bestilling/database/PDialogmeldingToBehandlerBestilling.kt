package no.nav.syfo.dialogmelding.bestilling.database

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.dialogmelding.bestilling.domain.*
import no.nav.syfo.domain.Personident
import java.sql.Timestamp
import java.util.UUID

data class PDialogmeldingToBehandlerBestilling(
    val id: Int,
    val uuid: UUID,
    val behandlerId: Int,
    val arbeidstakerPersonident: String,
    val parentRef: String?,
    val conversationUuid: UUID,
    val type: String,
    val kodeverk: String?,
    val kode: Int,
    val tekst: String?,
    val vedlegg: ByteArray? = null,
    val kilde: String?,
    val sendt: Timestamp?,
    val sendtTries: Int,
)

fun PDialogmeldingToBehandlerBestilling.toDialogmeldingToBehandlerBestilling(
    behandler: Behandler,
) = DialogmeldingToBehandlerBestilling(
    uuid = this.uuid,
    behandler = behandler,
    arbeidstakerPersonident = Personident(this.arbeidstakerPersonident),
    parentRef = this.parentRef,
    conversationUuid = this.conversationUuid,
    type = DialogmeldingType.valueOf(this.type),
    kodeverk = this.kodeverk?.let { DialogmeldingKodeverk.valueOf(it) },
    kode = DialogmeldingKode.fromInt(this.kode),
    tekst = this.tekst,
    vedlegg = this.vedlegg,
    kilde = kilde,
)
