package no.nav.syfo.dialogmelding

import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import no.nav.syfo.fellesformat.Fellesformat
import no.nav.syfo.dialogmelding.converter.createFellesformat
import no.nav.syfo.util.JAXB
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.syfo.dialogmelding")

class DialogmeldingService(
    val behandlerDialogmeldingService: BehandlerDialogmeldingService,
    val mqSender: MQSender,
) {
    suspend fun sendMelding(melding: BehandlerDialogmeldingBestilling) {
        log.info("Sending dialogmelding to lege with partnerId: ${melding.behandler.kontor.partnerId}")
        val arbeidstaker = behandlerDialogmeldingService.getBehandlerDialogmeldingArbeidstaker(
            melding.behandler.behandlerRef,
            melding.arbeidstakerPersonIdent,
        )

        val fellesformat: Fellesformat = opprettDialogmelding(melding, arbeidstaker)

        mqSender.sendMessageToEmottak(fellesformat.message!!)
    }

    private fun opprettDialogmelding(
        melding: BehandlerDialogmeldingBestilling,
        arbeidstaker: BehandlerDialogmeldingArbeidstaker,
    ): Fellesformat {
        val xmleiFellesformat = createFellesformat(
            melding = melding,
            arbeidstaker = arbeidstaker,
        )
        return Fellesformat(xmleiFellesformat, JAXB::marshallDialogmelding1_0)
    }
}
