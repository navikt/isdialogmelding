package no.nav.syfo.oppfolgingsplan

import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.api.person.RSOppfolgingsplan
import no.nav.syfo.behandler.domain.DialogmeldingKode
import no.nav.syfo.behandler.domain.DialogmeldingType
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.DialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.domain.Personident
import no.nav.syfo.metric.COUNT_SEND_OPPFOLGINGSPLAN_FAILED
import no.nav.syfo.metric.COUNT_SEND_OPPFOLGINGSPLAN_SUCCESS
import no.nav.syfo.oppfolgingsplan.exception.FastlegeNotFoundException
import java.util.UUID

class OppfolgingsplanService(
    val behandlerService: BehandlerService,
    val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
) {
    suspend fun sendOppfolgingsplan(
        callId: String,
        token: String,
        oppfolgingsplan: RSOppfolgingsplan,
    ) {
        try {
            val arbeidstakerIdent = Personident(oppfolgingsplan.sykmeldtFnr)
            val fastlegeBehandler = behandlerService.getFastlegeBehandler(
                personident = arbeidstakerIdent,
                token = token,
                callId = callId,
                systemRequest = true,
            ) ?: throw FastlegeNotFoundException("Feil ved sending av oppfølgingsplan, FastlegeIkkeFunnet")

            val arbeidstaker = dialogmeldingToBehandlerService.getArbeidstakerIfRelasjonToBehandler(
                behandlerRef = fastlegeBehandler.behandlerRef,
                personident = arbeidstakerIdent,
            )
            lagreDialogmeldingBestilling(
                oppfolgingsplan = oppfolgingsplan,
                behandlerRef = fastlegeBehandler.behandlerRef,
                arbeidstakerPersonIdent = arbeidstaker.arbeidstakerPersonident,
            )
            COUNT_SEND_OPPFOLGINGSPLAN_SUCCESS.increment()
        } catch (exc: Exception) {
            COUNT_SEND_OPPFOLGINGSPLAN_FAILED.increment()
            throw exc
        }
    }

    fun lagreDialogmeldingBestilling(
        oppfolgingsplan: RSOppfolgingsplan,
        behandlerRef: UUID,
        arbeidstakerPersonIdent: Personident,
    ) {
        val dialogmeldingToBehandlerBestillingDTO = DialogmeldingToBehandlerBestillingDTO(
            behandlerRef = behandlerRef.toString(),
            personIdent = arbeidstakerPersonIdent.value,
            dialogmeldingUuid = UUID.randomUUID().toString(),
            dialogmeldingRefParent = null, // brukes ikke for oppfølgingsplan
            dialogmeldingRefConversation = UUID.randomUUID().toString(), // brukes ikke for oppfølgingsplan
            dialogmeldingType = DialogmeldingType.OPPFOLGINGSPLAN.name,
            dialogmeldingKode = DialogmeldingKode.KODE1.value, // brukes ikke for oppfølgingsplan
            dialogmeldingTekst = null, // brukes ikke for oppfølgingsplan
            dialogmeldingVedlegg = oppfolgingsplan.oppfolgingsplanPdf,
        )
        dialogmeldingToBehandlerService.handleIncomingDialogmeldingBestilling(
            dialogmeldingToBehandlerBestillingDTO
        )
    }
}
