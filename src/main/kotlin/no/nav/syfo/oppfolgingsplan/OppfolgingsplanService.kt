package no.nav.syfo.oppfolgingsplan

import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.api.person.RSOppfolgingsplan
import no.nav.syfo.dialogmelding.bestilling.kafka.DialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.dialogmelding.bestilling.domain.*
import no.nav.syfo.domain.Personident
import no.nav.syfo.metric.COUNT_SEND_OPPFOLGINGSPLAN_FAILED
import no.nav.syfo.metric.COUNT_SEND_OPPFOLGINGSPLAN_SUCCESS
import no.nav.syfo.oppfolgingsplan.exception.FastlegeNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class OppfolgingsplanService(
    val behandlerService: BehandlerService,
    val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
) {
    private val log: Logger = LoggerFactory.getLogger(OppfolgingsplanService::class.java)

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
            )
            val vikarBehandler = if (fastlegeBehandler == null) {
                behandlerService.getFastlegevikarBehandler(
                    personident = arbeidstakerIdent,
                    token = token,
                    callId = callId,
                )
            } else null

            if (fastlegeBehandler == null && vikarBehandler == null) {
                throw FastlegeNotFoundException("Feil ved sending av oppfølgingsplan, FastlegeIkkeFunnet")
            }

            val behandlerRef = fastlegeBehandler?.behandlerRef ?: vikarBehandler!!.behandlerRef
            val arbeidstaker = dialogmeldingToBehandlerService.getArbeidstakerIfRelasjonToBehandler(
                behandlerRef = behandlerRef,
                personident = arbeidstakerIdent,
            )
            val dialogmeldingToBehandlerBestilling = lagreDialogmeldingBestilling(
                oppfolgingsplan = oppfolgingsplan,
                behandlerRef = behandlerRef,
                arbeidstakerPersonIdent = arbeidstaker.arbeidstakerPersonident,
            ) ?: throw RuntimeException("Lagring av bestilling for oppfølgingsplan feilet")

            COUNT_SEND_OPPFOLGINGSPLAN_SUCCESS.increment()
            val vikar = (fastlegeBehandler == null && vikarBehandler != null)
            log.info("Lagret bestilling for oppfølgingsplan til ${if (vikar) "fastlegevikar" else "fastlege"}, bestillingid: ${dialogmeldingToBehandlerBestilling.uuid}")
        } catch (exc: Exception) {
            COUNT_SEND_OPPFOLGINGSPLAN_FAILED.increment()
            throw exc
        }
    }

    fun lagreDialogmeldingBestilling(
        oppfolgingsplan: RSOppfolgingsplan,
        behandlerRef: UUID,
        arbeidstakerPersonIdent: Personident,
    ): DialogmeldingToBehandlerBestilling? {
        val dialogmeldingToBehandlerBestillingDTO = DialogmeldingToBehandlerBestillingDTO(
            behandlerRef = behandlerRef.toString(),
            personIdent = arbeidstakerPersonIdent.value,
            dialogmeldingUuid = UUID.randomUUID().toString(),
            dialogmeldingRefParent = null, // brukes ikke for oppfølgingsplan
            dialogmeldingRefConversation = UUID.randomUUID().toString(), // brukes ikke for oppfølgingsplan
            dialogmeldingType = DialogmeldingType.OPPFOLGINGSPLAN.name,
            dialogmeldingKodeverk = DialogmeldingKodeverk.HENVENDELSE.name,
            dialogmeldingKode = DialogmeldingKode.KODE1.value,
            dialogmeldingTekst = null, // brukes ikke for oppfølgingsplan
            dialogmeldingVedlegg = oppfolgingsplan.oppfolgingsplanPdf,
            kilde = "ESYFO",
        )
        return dialogmeldingToBehandlerService.handleIncomingDialogmeldingBestilling(
            dialogmeldingToBehandlerBestillingDTO
        )
    }
}
