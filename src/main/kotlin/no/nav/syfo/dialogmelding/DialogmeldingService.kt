package no.nav.syfo.dialogmelding

import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.domain.*
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
        log.info("Sending dialogmelding to lege with partnerId: ${melding.behandler.kontor.partnerId}")
        val arbeidstaker = dialogmeldingToBehandlerService.getArbeidstakerIfRelasjonToBehandler(
            melding.behandler.behandlerRef,
            melding.arbeidstakerPersonident,
        )

        val fellesformat: Fellesformat = opprettDialogmelding(melding, arbeidstaker)

        mqSender.sendMessageToEmottak(fellesformat.message!!)
    }

    private fun opprettDialogmelding(
        melding: DialogmeldingToBehandlerBestilling,
        arbeidstaker: Arbeidstaker,
    ): Fellesformat {
        val xmleiFellesformat = createFellesformat(
            melding = melding,
            arbeidstaker = arbeidstaker,
        )
        return Fellesformat(xmleiFellesformat, JAXB::marshallDialogmelding1_0)
    }
}
