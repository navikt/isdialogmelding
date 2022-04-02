package no.nav.syfo.dialogmelding

import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.fellesformat.Fellesformat
import no.nav.syfo.dialogmelding.converter.createFellesformat
import no.nav.syfo.util.JAXB
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.syfo.dialogmelding")

class DialogmeldingService(
    val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    val mqSender: MQSender,
) {
    suspend fun sendMelding(melding: DialogmeldingToBehandlerBestilling) {
        log.info("Sending dialogmelding to lege with partnerId: ${melding.behandler.partnerId}")
        val arbeidstaker = dialogmeldingToBehandlerService.getBehandlerDialogmeldingArbeidstaker(
            melding.behandler.behandlerRef,
            melding.arbeidstakerPersonIdent,
        )

        val fellesformat: Fellesformat = opprettDialogmelding(melding, arbeidstaker)

        mqSender.sendMessageToEmottak(fellesformat.message!!)
    }

    private fun opprettDialogmelding(
        melding: DialogmeldingToBehandlerBestilling,
        arbeidstaker: BehandlerDialogmeldingArbeidstaker,
    ): Fellesformat {
        val xmleiFellesformat = createFellesformat(
            melding = melding,
            arbeidstaker = arbeidstaker,
        )
        return Fellesformat(xmleiFellesformat, JAXB::marshallDialogmelding1_0)
    }
}
